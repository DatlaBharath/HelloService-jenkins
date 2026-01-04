pipeline {
    agent any

    stages {

        /* -------- SETUP KUBERNETES ENVIRONMENT WITH POSTGRESQL ----- */
        stage('Setup Kubernetes Environment') {
            steps {
                sh '''
ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.110.187.228 << 'EOF'
set -e

echo "===== Waiting for apt locks ====="
while sudo fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1; do
    echo "Waiting for apt lock..."
    sleep 5
done

sudo systemctl stop unattended-upgrades || true

echo "===== Update OS ====="
sudo apt-get update -y

echo "===== Install prerequisites ====="
sudo apt-get install -y ca-certificates curl gnupg lsb-release

echo "===== Install Docker (safe & idempotent) ====="
if ! command -v docker >/dev/null 2>&1; then
    sudo apt-get remove -y docker docker-engine docker.io containerd runc || true

    if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
        sudo mkdir -p /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
            sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    fi

    if [ ! -f /etc/apt/sources.list.d/docker.list ]; then
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
        https://download.docker.com/linux/ubuntu \
        $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    fi

    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
    sudo systemctl enable docker
    sudo systemctl start docker
fi

echo "===== Add user to docker group ====="
sudo usermod -aG docker ubuntu

echo "===== Install PostgreSQL ====="
if ! command -v psql >/dev/null 2>&1; then
    sudo apt-get install -y postgresql postgresql-contrib
    sudo systemctl enable postgresql
    sudo systemctl start postgresql
    
    # Configure PostgreSQL to accept connections
    sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" /etc/postgresql/*/main/postgresql.conf
    echo "host    all             all             0.0.0.0/0               md5" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf
    
    # Create default user and database
    sudo -u postgres psql -c "CREATE USER appuser WITH PASSWORD 'apppassword';" || true
    sudo -u postgres psql -c "CREATE DATABASE appdb OWNER appuser;" || true
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;" || true
    
    sudo systemctl restart postgresql
fi

echo "===== Install Redis ====="
if ! command -v redis-server >/dev/null 2>&1; then
    sudo apt-get install -y redis-server
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
fi

echo "===== Install Azure CLI ====="
if ! command -v az >/dev/null 2>&1; then
    curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
fi

echo "===== Install Terraform ====="
if ! command -v terraform >/dev/null 2>&1; then
    # Install required packages
    sudo apt-get install -y wget unzip
    
    # Add HashiCorp GPG key
    wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
    
    # Add HashiCorp repository
    echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
        sudo tee /etc/apt/sources.list.d/hashicorp.list
    
    # Update and install Terraform
    sudo apt-get update -y
    sudo apt-get install -y terraform
fi

echo "===== Install kubectl ====="
if ! command -v kubectl >/dev/null 2>&1; then
    curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
    chmod +x kubectl
    sudo mv kubectl /usr/local/bin/
fi

echo "===== Install Minikube ====="
if ! command -v minikube >/dev/null 2>&1; then
    curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
    chmod +x minikube-linux-amd64
    sudo mv minikube-linux-amd64 /usr/local/bin/minikube
fi

echo "===== Stop any existing Minikube instance ====="
sudo minikube delete --all --purge || true

echo "===== Start Minikube with sudo (workaround for docker group) ====="
sudo minikube start --driver=docker --force

echo "===== Enable Minikube addons ====="
sudo minikube addons enable metrics-server || echo "⚠️  metrics-server addon failed"
sudo minikube addons enable ingress || echo "⚠️  ingress addon failed"

echo "===== Wait for addons to initialize ====="
sleep 10

echo "===== Verify enabled addons ====="
sudo minikube addons list | grep -E "metrics-server|ingress"

echo "===== Configure kubectl for ubuntu user ====="
# Copy minikube certificates to ubuntu user directory
sudo mkdir -p /home/ubuntu/.minikube
sudo cp -r /root/.minikube/ca.crt /home/ubuntu/.minikube/ 2>/dev/null || true
sudo cp -r /root/.minikube/profiles /home/ubuntu/.minikube/ 2>/dev/null || true
sudo chown -R ubuntu:ubuntu /home/ubuntu/.minikube

# Setup kubectl config for ubuntu user
mkdir -p /home/ubuntu/.kube
sudo cp /root/.kube/config /home/ubuntu/.kube/config 2>/dev/null || sudo minikube kubectl -- config view --raw > /home/ubuntu/.kube/config

# Update certificate paths in kubeconfig to point to ubuntu user directory
sed -i "s|/root/.minikube|/home/ubuntu/.minikube|g" /home/ubuntu/.kube/config

sudo chown -R ubuntu:ubuntu /home/ubuntu/.kube
export KUBECONFIG=/home/ubuntu/.kube/config

echo "===== Configure kubectl for root user (fix kubeconfig) ====="
# Regenerate root's kubeconfig from minikube to ensure it's correct
sudo minikube update-context
sudo chmod 644 /root/.kube/config

echo "===== Verify root kubectl access ====="
sudo kubectl cluster-info
sudo kubectl get nodes

echo "===== Create and configure namespace ====="
kubectl create namespace unified-ns --dry-run=client -o yaml | kubectl apply -f -

echo "===== Set default namespace in kubeconfig ====="
# Set namespace for ubuntu user
kubectl config set-context --current --namespace=unified-ns
CURRENT_CONTEXT=$(kubectl config current-context)
kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns

# Set namespace for root user
sudo kubectl config set-context --current --namespace=unified-ns
sudo kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns

# Verify and update kubeconfig if needed
if ! grep -q "namespace: unified-ns" /home/ubuntu/.kube/config; then
    echo "Updating ubuntu kubeconfig to add namespace..."
    kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns
fi

if ! sudo grep -q "namespace: unified-ns" /root/.kube/config; then
    echo "Updating root kubeconfig to add namespace..."
    sudo kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns
fi

echo "===== Verify namespace configuration ====="
echo "Current context:"
kubectl config current-context
echo ""
echo "Current namespace (ubuntu user):"
kubectl config view --minify --output 'jsonpath={..namespace}'
echo ""
echo ""
echo "Current namespace (root user):"
sudo kubectl config view --minify --output 'jsonpath={..namespace}'
echo ""
echo ""
echo "Namespace details:"
kubectl get namespace unified-ns
echo ""
echo "Testing kubectl get pods for ubuntu user:"
kubectl get pods 2>&1 | head -3
echo ""
echo "Testing kubectl get pods for root user:"
sudo kubectl get pods 2>&1 | head -3

echo "===== Wait for ingress controller to be ready ====="
echo "Waiting for ingress-nginx controller..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s || echo "⚠️  Ingress controller not ready yet (might need more time)"

echo "===== Verify Kubernetes cluster ====="
kubectl cluster-info
kubectl get nodes
kubectl get pods --all-namespaces

echo "===== Verify Setup ====="
echo ""
echo "Docker Version:"
sudo docker --version
echo ""
echo "PostgreSQL Version:"
psql --version
echo ""
echo "Redis Status:"
redis-cli ping
echo ""
echo "Terraform Version:"
terraform version
echo ""
echo "Kubectl Version:"
kubectl version --client
echo ""
echo "Minikube Status:"
sudo minikube status
echo ""

echo "===== PostgreSQL Connection Info ====="
echo "PostgreSQL is installed and running on localhost:5432"
echo "Database: appdb"
echo "User: appuser"
echo "Password: apppassword"

echo ""
echo "===== Kubernetes Cluster Info ====="
echo "Namespace: unified-ns"
echo "Addons enabled: metrics-server, ingress"
echo ""
echo "✅ Kubernetes Environment Setup Complete!"
EOF
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Setup was successful'
        }
        failure {
            echo '❌ Setup failed'
        }
    }
}

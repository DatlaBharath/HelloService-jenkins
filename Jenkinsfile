pipeline {
    agent any

    stages {

        /* -------- SETUP KUBERNETES ENVIRONMENT WITH POSTGRESQL ----- */
        stage('Setup Kubernetes Environment') {
            steps {
                sh '''
ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@43.204.22.1 << 'EOF'
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
sudo minikube addons enable metrics-server
sudo minikube addons enable ingress

echo "===== Configure kubectl for ubuntu user ====="
sudo chmod 644 /root/.kube/config || true
mkdir -p /home/ubuntu/.kube
sudo cp /root/.kube/config /home/ubuntu/.kube/config 2>/dev/null || sudo minikube kubectl -- config view --raw > /home/ubuntu/.kube/config
sudo chown -R ubuntu:ubuntu /home/ubuntu/.kube
export KUBECONFIG=/home/ubuntu/.kube/config

echo "===== Verify Setup ====="
sudo docker --version
psql --version
redis-cli ping
kubectl version --client
sudo minikube status

echo "===== Create and configure namespace ====="
kubectl create namespace unified-ns --dry-run=client -o yaml | kubectl apply -f -
kubectl config set-context --current --namespace=unified-ns
echo "Current namespace: $(kubectl config view --minify --output 'jsonpath={..namespace}')"

echo "===== PostgreSQL Connection Info ====="
echo "PostgreSQL is installed and running on localhost:5432"
echo "Database: appdb"
echo "User: appuser"
echo "Password: apppassword"

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

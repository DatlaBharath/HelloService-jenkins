pipeline {
    agent any
    
    environment {
        // Terraform workspace directory
        TF_DIR = "${WORKSPACE}/terraform"
        // EC2 Public IP will be captured here
        EC2_PUBLIC_IP = ''
        // AWS credentials (configured in Jenkins)
        AWS_DEFAULT_REGION = 'ap-south-1'
        // Path to your existing SSH key
        SSH_KEY_PATH = '/var/test.pem'
        SSH_KEY_NAME = 'test'  // Name of the key pair in AWS
    }

    stages {
        
        /* -------- PROVISION EC2 INSTANCE WITH TERRAFORM -------- */
        stage('Provision EC2 Instance (Terraform)') {
            steps {
                script {
                    echo "===== Creating Terraform Configuration ====="
                    
                    // Create terraform directory
                    sh "mkdir -p ${TF_DIR}"
                    
                    // Write Terraform configuration files
                    writeFile file: "${TF_DIR}/main.tf", text: '''
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Security Group for the EC2 instance
resource "aws_security_group" "k8s_instance_sg" {
  name        = "k8s-instance-sg-${var.environment}"
  description = "Security group for Kubernetes EC2 instance"

  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access"
  }

  # PostgreSQL
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "PostgreSQL"
  }

  # Redis
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Redis"
  }

  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS"
  }

  # Kubernetes NodePort range
  ingress {
    from_port   = 30000
    to_port     = 32767
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Kubernetes NodePort services"
  }

  # All outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound"
  }

  tags = {
    Name        = "k8s-instance-sg-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# EC2 Instance
resource "aws_instance" "k8s_instance" {
  ami           = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name

  vpc_security_group_ids = [aws_security_group.k8s_instance_sg.id]

  root_block_device {
    volume_size = var.volume_size
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
              #!/bin/bash
              # Wait for cloud-init to complete
              cloud-init status --wait
              
              # Disable unattended upgrades to prevent apt lock issues
              systemctl stop unattended-upgrades
              systemctl disable unattended-upgrades
              
              # Update hostname
              hostnamectl set-hostname k8s-node
              EOF

  tags = {
    Name        = "k8s-instance-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "Kubernetes-Setup"
  }

  # Wait for instance to be fully ready
  provisioner "local-exec" {
    command = "sleep 30"
  }
}
'''

                    writeFile file: "${TF_DIR}/variables.tf", text: '''
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "ami_id" {
  description = "Ubuntu AMI ID for the region"
  type        = string
  # Ubuntu 22.04 LTS for ap-south-1 (Mumbai)
  default     = "ami-0dee22c13ea7a9a67"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.large"
}

variable "key_name" {
  description = "Name of the SSH key pair"
  type        = string
}

variable "volume_size" {
  description = "Root volume size in GB"
  type        = number
  default     = 30
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}
'''

                    writeFile file: "${TF_DIR}/outputs.tf", text: '''
output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.k8s_instance.id
}

output "public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.k8s_instance.public_ip
}

output "private_ip" {
  description = "Private IP of the EC2 instance"
  value       = aws_instance.k8s_instance.private_ip
}

output "security_group_id" {
  description = "ID of the security group"
  value       = aws_security_group.k8s_instance_sg.id
}
'''

                    writeFile file: "${TF_DIR}/terraform.tfvars", text: """
aws_region    = "${AWS_DEFAULT_REGION}"
key_name      = "${SSH_KEY_NAME}"
instance_type = "t3.large"
volume_size   = 30
environment   = "jenkins-${BUILD_NUMBER}"
"""

                    echo "===== Terraform Init ====="
                    dir("${TF_DIR}") {
                        sh 'terraform init'
                    }
                    
                    echo "===== Terraform Validate ====="
                    dir("${TF_DIR}") {
                        sh 'terraform validate'
                    }
                    
                    echo "===== Terraform Plan ====="
                    dir("${TF_DIR}") {
                        sh 'terraform plan -out=tfplan'
                    }
                    
                    echo "===== Terraform Apply ====="
                    dir("${TF_DIR}") {
                        sh 'terraform apply -auto-approve tfplan'
                    }
                    
                    echo "===== Capturing EC2 Public IP ====="
                    dir("${TF_DIR}") {
                        EC2_PUBLIC_IP = sh(
                            script: 'terraform output -raw public_ip',
                            returnStdout: true
                        ).trim()
                    }
                    
                    echo "✅ EC2 Instance Provisioned Successfully!"
                    echo "📍 Public IP: ${EC2_PUBLIC_IP}"
                    
                    // Wait for instance to be fully ready for SSH
                    echo "===== Waiting for SSH to be ready ====="
                    sh """
                        for i in {1..30}; do
                            if ssh -i ${SSH_KEY_PATH} -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@${EC2_PUBLIC_IP} 'echo SSH Ready' 2>/dev/null; then
                                echo "✅ SSH is ready!"
                                break
                            fi
                            echo "Waiting for SSH... (attempt \$i/30)"
                            sleep 10
                        done
                    """
                }
            }
        }

        /* -------- SETUP KUBERNETES ENVIRONMENT WITH POSTGRESQL ----- */
        stage('Setup Kubernetes Environment') {
            steps {
                script {
                    echo "===== Installing Dependencies on EC2: ${EC2_PUBLIC_IP} ====="
                    
                    sh """
ssh -i ${SSH_KEY_PATH} -o StrictHostKeyChecking=no ubuntu@${EC2_PUBLIC_IP} << 'EOF'
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
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \\
            sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    fi

    if [ ! -f /etc/apt/sources.list.d/docker.list ]; then
        echo "deb [arch=\\$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \\
        https://download.docker.com/linux/ubuntu \\
        \\$(lsb_release -cs) stable" | \\
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
    curl -LO https://storage.googleapis.com/kubernetes-release/release/\\$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
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
CURRENT_CONTEXT=\\$(kubectl config current-context)
kubectl config set-context \\$CURRENT_CONTEXT --namespace=unified-ns

# Set namespace for root user
sudo kubectl config set-context --current --namespace=unified-ns
sudo kubectl config set-context \\$CURRENT_CONTEXT --namespace=unified-ns

# Verify and update kubeconfig if needed
if ! grep -q "namespace: unified-ns" /home/ubuntu/.kube/config; then
    echo "Updating ubuntu kubeconfig to add namespace..."
    kubectl config set-context \\$CURRENT_CONTEXT --namespace=unified-ns
fi

if ! sudo grep -q "namespace: unified-ns" /root/.kube/config; then
    echo "Updating root kubeconfig to add namespace..."
    sudo kubectl config set-context \\$CURRENT_CONTEXT --namespace=unified-ns
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
kubectl wait --namespace ingress-nginx \\
  --for=condition=ready pod \\
  --selector=app.kubernetes.io/component=controller \\
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
                    """
                }
            }
        }
        
        /* -------- DISPLAY SETUP SUMMARY -------- */
        stage('Display Setup Summary') {
            steps {
                script {
                    echo """
                    ╔═══════════════════════════════════════════════════════════╗
                    ║           🎉 DEPLOYMENT SUMMARY 🎉                        ║
                    ╚═══════════════════════════════════════════════════════════╝
                    
                    📍 EC2 Instance Public IP: ${EC2_PUBLIC_IP}
                    🔐 SSH Access: ssh -i ${SSH_KEY_PATH} ubuntu@${EC2_PUBLIC_IP}
                    
                    📦 Installed Components:
                       ✅ Docker
                       ✅ Kubernetes (Minikube)
                       ✅ PostgreSQL (Port 5432)
                       ✅ Redis (Port 6379)
                       ✅ Azure CLI
                       ✅ kubectl
                    
                    🗄️  PostgreSQL Connection:
                       Host: ${EC2_PUBLIC_IP}
                       Port: 5432
                       Database: appdb
                       User: appuser
                       Password: apppassword
                    
                    ☸️  Kubernetes:
                       Namespace: unified-ns
                       Addons: metrics-server, ingress
                    
                    🔗 Access Commands:
                       kubectl --kubeconfig=/home/ubuntu/.kube/config get pods
                       psql -h ${EC2_PUBLIC_IP} -U appuser -d appdb
                    
                    ╔═══════════════════════════════════════════════════════════╗
                    ║  Infrastructure is ready for application deployment! 🚀  ║
                    ╚═══════════════════════════════════════════════════════════╝
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            echo "📍 EC2 Instance: ${EC2_PUBLIC_IP}"
            echo "💡 Remember to run 'terraform destroy' when done to avoid AWS charges"
        }
        failure {
            echo '❌ Pipeline failed!'
            echo '🔍 Check the logs above for error details'
        }
        cleanup {
            echo '🧹 Cleaning up workspace...'
            // Uncomment the line below to automatically destroy infrastructure on pipeline completion
            // sh "cd ${TF_DIR} && terraform destroy -auto-approve"
        }
    }
}

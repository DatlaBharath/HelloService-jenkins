pipeline {
    agent any

    environment {
        TF_DIR = "${WORKSPACE}/terraform"
        EC2_PUBLIC_IP = ""
        AWS_DEFAULT_REGION = "ap-south-1"
        SSH_KEY_PATH = "/var/test.pem"
        SSH_KEY_NAME = "test"
    }

    stages {
        
        /* -------- INSTALL TERRAFORM -------- */
        stage('Install Terraform') {
            steps {
                sh '''
                if ! command -v terraform >/dev/null 2>&1; then
                    echo "===== Installing Terraform ====="
                    
                    # Install prerequisites
                    apt-get update -y
                    apt-get install -y gnupg curl lsb-release software-properties-common wget unzip
                    
                    # Download and install Terraform
                    TERRAFORM_VERSION="1.7.0"
                    cd /tmp
                    wget https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
                    unzip -o terraform_${TERRAFORM_VERSION}_linux_amd64.zip
                    chmod +x terraform
                    mv terraform /usr/local/bin/
                    rm -f terraform_${TERRAFORM_VERSION}_linux_amd64.zip
                    
                    echo "✅ Terraform installed successfully"
                else
                    echo "✅ Terraform is already installed"
                fi
                
                echo "===== Terraform Version ====="
                terraform version
                '''
            }
        }

        /* -------- PROVISION EC2 INSTANCE WITH TERRAFORM -------- */
        stage('Provision EC2 Instance (Terraform)') {
            steps {
                script {
                    echo "===== Creating Terraform Configuration ====="
                    
                    sh "mkdir -p ${TF_DIR}"

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

# Security Group
resource "aws_security_group" "k8s_sg" {
  name        = "k8s-sg-${var.environment}"
  description = "Security group for Kubernetes EC2 instance"

  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH"
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

  # Kubernetes NodePort
  ingress {
    from_port   = 30000
    to_port     = 32767
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Kubernetes NodePort"
  }

  # All outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "k8s-sg-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# EC2 Instance
resource "aws_instance" "k8s" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name              = var.key_name
  vpc_security_group_ids = [aws_security_group.k8s_sg.id]

  root_block_device {
    volume_size = var.volume_size
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              cloud-init status --wait
              systemctl stop unattended-upgrades || true
              systemctl disable unattended-upgrades || true
              hostnamectl set-hostname k8s-node
              EOF

  tags = {
    Name        = "k8s-instance-${var.environment}"
    Environment = var.environment
    ManagedBy   = "Terraform"
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
  description = "Ubuntu AMI ID"
  type        = string
  default     = "ami-0dee22c13ea7a9a67"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.large"
}

variable "key_name" {
  description = "SSH key pair name"
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
  description = "EC2 instance ID"
  value       = aws_instance.k8s.id
}

output "public_ip" {
  description = "EC2 public IP"
  value       = aws_instance.k8s.public_ip
}

output "private_ip" {
  description = "EC2 private IP"
  value       = aws_instance.k8s.private_ip
}

output "security_group_id" {
  description = "Security group ID"
  value       = aws_security_group.k8s_sg.id
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
                    
                    echo "✅ EC2 Instance Provisioned!"
                    echo "📍 Public IP: ${EC2_PUBLIC_IP}"
                    
                    echo "===== Waiting for SSH to be ready ====="
                    sh """
                        for i in {1..30}; do
                            if ssh -i ${SSH_KEY_PATH} -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@${EC2_PUBLIC_IP} 'echo SSH Ready' 2>/dev/null; then
                                echo "✅ SSH is ready!"
                                break
                            fi
                            echo "⏳ Waiting for SSH... (attempt \$i/30)"
                            sleep 10
                        done
                    """
                }
            }
        }

        /* -------- SETUP KUBERNETES ENVIRONMENT -------- */
        stage('Setup Kubernetes Environment') {
            steps {
                script {
                    echo "===== Installing Dependencies on EC2: ${EC2_PUBLIC_IP} ====="
                    
                    def setupScript = '''#!/bin/bash
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

echo "===== Install Docker ====="
if ! command -v docker >/dev/null 2>&1; then
    sudo apt-get remove -y docker docker-engine docker.io containerd runc || true
    
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker ubuntu
    echo "✅ Docker installed"
else
    echo "✅ Docker already installed"
fi

echo "===== Install PostgreSQL ====="
if ! command -v psql >/dev/null 2>&1; then
    sudo apt-get install -y postgresql postgresql-contrib
    sudo systemctl enable postgresql
    sudo systemctl start postgresql
    
    sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" /etc/postgresql/*/main/postgresql.conf
    echo "host    all             all             0.0.0.0/0               md5" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf
    
    sudo -u postgres psql -c "CREATE USER appuser WITH PASSWORD 'apppassword';" || true
    sudo -u postgres psql -c "CREATE DATABASE appdb OWNER appuser;" || true
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE appdb TO appuser;" || true
    
    sudo systemctl restart postgresql
    echo "✅ PostgreSQL installed"
else
    echo "✅ PostgreSQL already installed"
fi

echo "===== Install Redis ====="
if ! command -v redis-server >/dev/null 2>&1; then
    sudo apt-get install -y redis-server
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
    echo "✅ Redis installed"
else
    echo "✅ Redis already installed"
fi

echo "===== Install Azure CLI ====="
if ! command -v az >/dev/null 2>&1; then
    curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
    echo "✅ Azure CLI installed"
else
    echo "✅ Azure CLI already installed"
fi

echo "===== Install kubectl ====="
if ! command -v kubectl >/dev/null 2>&1; then
    curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
    chmod +x kubectl
    sudo mv kubectl /usr/local/bin/
    echo "✅ kubectl installed"
else
    echo "✅ kubectl already installed"
fi

echo "===== Install Minikube ====="
if ! command -v minikube >/dev/null 2>&1; then
    curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
    chmod +x minikube-linux-amd64
    sudo mv minikube-linux-amd64 /usr/local/bin/minikube
    echo "✅ Minikube installed"
else
    echo "✅ Minikube already installed"
fi

echo "===== Stop any existing Minikube ====="
sudo minikube delete --all --purge || true

echo "===== Start Minikube ====="
sudo minikube start --driver=docker --force

echo "===== Enable Minikube addons ====="
sudo minikube addons enable metrics-server || echo "⚠️  metrics-server addon failed"
sudo minikube addons enable ingress || echo "⚠️  ingress addon failed"

sleep 10

echo "===== Configure kubectl ====="
sudo mkdir -p /home/ubuntu/.minikube
sudo cp -r /root/.minikube/ca.crt /home/ubuntu/.minikube/ 2>/dev/null || true
sudo cp -r /root/.minikube/profiles /home/ubuntu/.minikube/ 2>/dev/null || true
sudo chown -R ubuntu:ubuntu /home/ubuntu/.minikube

mkdir -p /home/ubuntu/.kube
sudo cp /root/.kube/config /home/ubuntu/.kube/config 2>/dev/null || sudo minikube kubectl -- config view --raw > /home/ubuntu/.kube/config
sed -i "s|/root/.minikube|/home/ubuntu/.minikube|g" /home/ubuntu/.kube/config
sudo chown -R ubuntu:ubuntu /home/ubuntu/.kube

export KUBECONFIG=/home/ubuntu/.kube/config

sudo minikube update-context
sudo chmod 644 /root/.kube/config

echo "===== Verify kubectl ====="
sudo kubectl cluster-info
sudo kubectl get nodes

echo "===== Create namespace ====="
kubectl create namespace unified-ns --dry-run=client -o yaml | kubectl apply -f -
kubectl config set-context --current --namespace=unified-ns

CURRENT_CONTEXT=$(kubectl config current-context)
kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns
sudo kubectl config set-context --current --namespace=unified-ns
sudo kubectl config set-context $CURRENT_CONTEXT --namespace=unified-ns

echo "===== Wait for ingress controller ====="
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s || echo "⚠️  Ingress controller may need more time"

echo "===== Final verification ====="
kubectl get nodes
kubectl get pods --all-namespaces

echo ""
echo "===== Setup Summary ====="
echo "Docker version: $(sudo docker --version)"
echo "PostgreSQL version: $(psql --version)"
echo "Redis status: $(redis-cli ping)"
echo "kubectl version: $(kubectl version --client --short)"
echo "Minikube status:"
sudo minikube status
echo ""
echo "✅ Kubernetes Environment Setup Complete!"
'''
                    
                    writeFile file: "${WORKSPACE}/setup.sh", text: setupScript
                    
                    sh """
                        scp -i ${SSH_KEY_PATH} -o StrictHostKeyChecking=no ${WORKSPACE}/setup.sh ubuntu@${EC2_PUBLIC_IP}:/tmp/setup.sh
                        ssh -i ${SSH_KEY_PATH} -o StrictHostKeyChecking=no ubuntu@${EC2_PUBLIC_IP} 'chmod +x /tmp/setup.sh && /tmp/setup.sh'
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
║              🎉 DEPLOYMENT SUMMARY 🎉                     ║
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
║     Infrastructure is ready for deployment! 🚀            ║
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
            echo "💡 To destroy infrastructure: cd ${TF_DIR} && terraform destroy -auto-approve"
        }
        failure {
            echo '❌ Pipeline failed!'
            echo '🔍 Check the logs above for details'
        }
        cleanup {
            echo '🧹 Cleaning up workspace...'
            // Uncomment to auto-destroy infrastructure:
            // dir("${TF_DIR}") {
            //     sh 'terraform destroy -auto-approve'
            // }
        }
    }
}

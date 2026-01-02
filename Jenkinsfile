pipeline {
    agent any

    environment {
        TF_DIR = "${WORKSPACE}/terraform"
        EC2_PUBLIC_IP = ""
        AWS_DEFAULT_REGION = "ap-south-1"
        SSH_KEY_PATH = "/var/test.pem"
        SSH_KEY_NAME = "test"
    }
    stage('Install Terraform') {
    steps {
        sh '''
        if ! command -v terraform >/dev/null 2>&1; then
            echo "Installing Terraform..."
            curl -fsSL https://apt.releases.hashicorp.com/gpg | gpg --dearmor | tee /usr/share/keyrings/hashicorp-archive-keyring.gpg >/dev/null
            echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/hashicorp.list
            apt-get update -y
            apt-get install -y terraform
        else
            echo "Terraform already installed"
        fi

        terraform version
        '''
    }
}

    stages {

        /* ------------------ TERRAFORM EC2 ------------------ */
        stage('Provision EC2 Instance (Terraform)') {
            steps {
                script {
                    sh '''
                    mkdir -p terraform
                    '''

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

resource "aws_security_group" "k8s_sg" {
  name = "k8s-sg-${var.environment}"

  ingress { from_port = 22 to_port = 22 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 5432 to_port = 5432 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 6379 to_port = 6379 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 80 to_port = 80 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 443 to_port = 443 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 30000 to_port = 32767 protocol = "tcp" cidr_blocks = ["0.0.0.0/0"] }

  egress { from_port = 0 to_port = 0 protocol = "-1" cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_instance" "k8s" {
  ami           = var.ami_id
  instance_type = var.instance_type
  key_name      = var.key_name
  vpc_security_group_ids = [aws_security_group.k8s_sg.id]

  root_block_device {
    volume_size = 30
  }

  tags = {
    Name = "k8s-${var.environment}"
  }
}
'''

                    writeFile file: "${TF_DIR}/variables.tf", text: '''
variable "aws_region" { default = "ap-south-1" }
variable "ami_id"     { default = "ami-0dee22c13ea7a9a67" }
variable "instance_type" { default = "t3.large" }
variable "key_name" {}
variable "environment" { default = "dev" }
'''

                    writeFile file: "${TF_DIR}/terraform.tfvars", text: """
aws_region  = "${AWS_DEFAULT_REGION}"
key_name    = "${SSH_KEY_NAME}"
environment = "jenkins-${BUILD_NUMBER}"
"""

                    dir("${TF_DIR}") {
                        sh '''
                        terraform init
                        terraform apply -auto-approve
                        '''
                        EC2_PUBLIC_IP = sh(
                            script: 'terraform output -raw public_ip',
                            returnStdout: true
                        ).trim()
                    }

                    echo "EC2 IP: ${EC2_PUBLIC_IP}"
                }
            }
        }

        /* ------------------ EC2 SETUP ------------------ */
        stage('Setup Kubernetes Environment') {
            steps {
                sh '''
ssh -i '"${SSH_KEY_PATH}"' -o StrictHostKeyChecking=no ubuntu@'"${EC2_PUBLIC_IP}"' << 'EOF'
set -e

sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# Docker
if ! command -v docker; then
  curl -fsSL https://get.docker.com | sudo bash
  sudo usermod -aG docker ubuntu
fi

# PostgreSQL
if ! command -v psql; then
  sudo apt-get install -y postgresql
  sudo systemctl enable postgresql
  sudo systemctl start postgresql
fi

# Redis
if ! command -v redis-server; then
  sudo apt-get install -y redis-server
  sudo systemctl enable redis-server
  sudo systemctl start redis-server
fi

# Azure CLI
if ! command -v az; then
  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
fi

# kubectl
if ! command -v kubectl; then
  curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
  chmod +x kubectl
  sudo mv kubectl /usr/local/bin/
fi

# Minikube
if ! command -v minikube; then
  curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
  chmod +x minikube-linux-amd64
  sudo mv minikube-linux-amd64 /usr/local/bin/minikube
fi

sudo minikube delete --all || true
sudo minikube start --driver=docker --force
sudo minikube addons enable ingress
sudo minikube addons enable metrics-server

kubectl create namespace unified-ns --dry-run=client -o yaml | kubectl apply -f -
kubectl config set-context --current --namespace=unified-ns

kubectl get nodes
kubectl get pods -A

echo "SETUP COMPLETE"
EOF
'''
            }
        }

        /* ------------------ SUMMARY ------------------ */
        stage('Display Summary') {
            steps {
                echo """
==============================
 EC2 IP: ${EC2_PUBLIC_IP}
 SSH: ssh -i ${SSH_KEY_PATH} ubuntu@${EC2_PUBLIC_IP}

 Installed:
  - Docker
  - Minikube
  - kubectl
  - PostgreSQL
  - Redis
  - Azure CLI

 Kubernetes Namespace: unified-ns
==============================
"""
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully"
        }
        failure {
            echo "Pipeline failed"
        }
    }
}

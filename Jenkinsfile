pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {

        /* -------------------- CHECKOUT -------------------- */
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/DatlaBharath/HelloService-jenkins'
            }
        }

        /* -------- SETUP KUBERNETES ENVIRONMENT (HARDENED) ----- */
        stage('Setup Kubernetes Environment') {
            steps {
                sh '''
ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@65.2.124.195 << 'EOF'
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
    sudo usermod -aG docker ubuntu
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

echo "===== Start Minikube ====="
minikube start --driver=docker || minikube start

echo "===== Verify Setup ====="
docker --version
redis-cli ping
kubectl version --client
minikube status
EOF
                '''
            }
        }

        /* -------------------- BUILD -------------------- */
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        /* ---------------- DOCKER BUILD ---------------- */
        stage('Build Docker Image') {
            steps {
                script {
                    def imageName = "sakthisiddu1/helloservice-jenkins:${env.BUILD_NUMBER}"
                    sh "docker build -t ${imageName} ."
                }
            }
        }

        /* ---------------- PUSH IMAGE ------------------ */
        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub_credentials',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) {
                        sh 'echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin'
                        def imageName = "sakthisiddu1/helloservice-jenkins:${env.BUILD_NUMBER}"
                        sh "docker push ${imageName}"
                    }
                }
            }
        }

        /* --------------- K8s DEPLOY ------------------- */
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh '''
cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: helloservice-jenkins-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: helloservice-jenkins
  template:
    metadata:
      labels:
        app: helloservice-jenkins
    spec:
      containers:
      - name: helloservice-jenkins
        image: sakthisiddu1/helloservice-jenkins:${BUILD_NUMBER}
        ports:
        - containerPort: 5000
EOF
                    '''

                    sh '''
cat <<EOF > service.yaml
apiVersion: v1
kind: Service
metadata:
  name: helloservice-jenkins-service
spec:
  type: NodePort
  selector:
    app: helloservice-jenkins
  ports:
  - port: 5000
    targetPort: 5000
    nodePort: 30007
EOF
                    '''

                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@65.2.124.195 "kubectl apply -f -" < deployment.yaml'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@65.2.124.195 "kubectl apply -f -" < service.yaml'
                }
            }
        }
    }

    post {
        success {
            echo '✅ Deployment was successful'
        }
        failure {
            echo '❌ Deployment failed'
        }
    }
}

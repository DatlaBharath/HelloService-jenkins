pipeline {
    agent any
    tools {
        maven 'Maven'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/DatlaBharath/HelloService-jenkins'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    def imageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                    sh "docker build -t ${imageName} ."
                }
            }
        }
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh """echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin"""
                    sh "docker push ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def deploymentYaml = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: helloservice
spec:
  replicas: 1
  selector:
    matchLabels:
      app: helloservice
  template:
    metadata:
      labels:
        app: helloservice
    spec:
      containers:
      - name: helloservice
        image: ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
        ports:
        - containerPort: 5000
"""
                    def serviceYaml = """
apiVersion: v1
kind: Service
metadata:
  name: helloservice
spec:
  selector:
    app: helloservice
  ports:
    - protocol: TCP
      port: 80
      targetPort: 5000
      nodePort: 30007
  type: NodePort
"""
                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml
                    sh 'scp -i /var/test.pem -o StrictHostKeyChecking=no deployment.yaml service.yaml ubuntu@13.232.84.241:/tmp/'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.232.84.241 "kubectl apply -f /tmp/deployment.yaml"'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.232.84.241 "kubectl apply -f /tmp/service.yaml"'
                }
            }
        }
    }
    post {
        success {
            echo 'Deployment succeeded'
        }
        failure {
            echo 'Deployment failed'
        }
    }
}
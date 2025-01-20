pipeline {
    agent any
    tools {
        maven 'Maven'
    }
    stages {
        stage('Checkout Source') {
            steps {
                git branch: 'main', url: 'https://github.com/DatlaBharath/HelloService-jenkins'
            }
        }
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Build Docker Image and Push') {
            steps {
                script {
                    def repoName = 'helloservice-jenkins'.toLowerCase()
                    def dockerTag = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """docker build -t ${dockerTag} ."""
                        sh """echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin"""
                        sh """docker push ${dockerTag}"""
                    }
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
  name: hello-service-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-service
  template:
    metadata:
      labels:
        app: hello-service
    spec:
      containers:
      - name: hello-service
        image: ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
        ports:
        - containerPort: 5000
"""
                    def serviceYaml = """
apiVersion: v1
kind: Service
metadata:
  name: hello-service
spec:
  type: NodePort
  selector:
    app: hello-service
  ports:
    - port: 5000
      targetPort: 5000
      nodePort: 30007
"""
                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml

                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml"""
                }
            }
        }
    }
    post {
        success {
            echo 'Deployment succeeded!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
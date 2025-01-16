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
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Create Docker Image') {
            steps {
                script {
                    def repoName = "helloservice-jenkins".toLowerCase()
                    def imageName = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"
                    sh "docker build -t ${imageName} ."
                }
            }
        }
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    script {
                        def repoName = "helloservice-jenkins".toLowerCase()
                        def imageName = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"
                        sh "echo ${env.PASSWORD} | docker login -u ${env.USERNAME} --password-stdin"
                        sh "docker push ${imageName}"
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def repoName = "helloservice-jenkins".toLowerCase()
                    def imageName = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"
                    def deploymentYaml = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-service
  labels:
    app: hello-service
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
        image: ${imageName}
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
  ports:
    - port: 5000
      nodePort: 30007
  selector:
    app: hello-service
"""
                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.111.149.219 "kubectl apply -f -" < deployment.yaml"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.111.149.219 "kubectl apply -f -" < service.yaml"""
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline succeeded.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
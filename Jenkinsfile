pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: 'main']],
                    userRemoteConfigs: [[url: 'https://github.com/DatlaBharath/HelloService-jenkins']]])
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build and Push') {
            steps {
                script {
                    def imageName = "ratneshpuskar/helloservice-jenkins"

                    sh "docker build -t ${imageName}:${env.BUILD_NUMBER} ."
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'echo $PASSWORD | docker login -u $USERNAME --password-stdin'
                        sh "docker push ${imageName}:${env.BUILD_NUMBER}"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def deploymentYAML = """
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
        image: "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
        ports:
        - containerPort: 5000
"""

                    def serviceYAML = """
apiVersion: v1
kind: Service
metadata:
  name: hello-service-service
spec:
  type: NodePort
  selector:
    app: hello-service
  ports:
    - port: 5000
      nodePort: 30007
"""

                    writeFile file: 'deployment.yaml', text: deploymentYAML
                    writeFile file: 'service.yaml', text: serviceYAML

                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml'
                }
            }
        }
    }

    post {
        success {
            echo 'Build and deployment succeeded'
        }
        failure {
            echo 'Build or deployment failed'
        }
    }
}
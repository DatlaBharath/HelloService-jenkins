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
        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME')]) {
                    script {
                        def imageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                        sh 'echo "${DOCKER_HUB_PASSWORD}" | docker login -u "${DOCKER_HUB_USERNAME}" --password-stdin'
                        sh "docker push ${imageName}"
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
  name: helloservice-jenkins
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
        image: ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
        ports:
        - containerPort: 5000
"""
                    def serviceYaml = """
apiVersion: v1
kind: Service
metadata:
  name: helloservice-jenkins
spec:
  type: NodePort
  selector:
    app: helloservice-jenkins
  ports:
    - protocol: TCP
      port: 5000
      targetPort: 5000
      nodePort: 30007
"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.234.111.163 "kubectl apply -f -" <<< '${deploymentYaml}'"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.234.111.163 "kubectl apply -f -" <<< '${serviceYaml}'"""
                }
            }
        }
    }
    post {
        success {
            echo 'Job succeeded!'
        }
        failure {
            echo 'Job failed!'
        }
    }
}
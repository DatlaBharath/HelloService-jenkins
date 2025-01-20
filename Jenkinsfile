pipeline {
    agent any 
    tools {
        maven 'Maven' 
    }
    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/DatlaBharath/HelloService-jenkins'
            }
        }
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    def dockerImage = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                    sh """
                    docker build -t ${dockerImage} .
                    """
                }
            }
        }
        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh """
                    echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
                    docker push ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
                    """
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def deployment = """
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
                            image: ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
                            ports:
                            - containerPort: 5000
                    """
                    def service = """
                    apiVersion: v1
                    kind: Service
                    metadata:
                      name: helloservice-jenkins-service
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
                    writeFile file: 'deployment.yaml', text: deployment
                    writeFile file: 'service.yaml', text: service
                    sh """
                    ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml
                    ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml
                    """
                }
            }
        }
    }
    post {
        success {
            echo 'Deployment completed successfully!'
        }
        failure {
            echo 'Deployment failed.'
        }
    }
}
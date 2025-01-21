pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Checkout code') {
            steps {
                git url: 'https://github.com/DatlaBharath/HelloService-jenkins', branch: 'main'
            }
        }
        
        stage('Build Maven project') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Build Docker image') {
            steps {
                script {
                    def dockerImageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                    sh "docker build -t ${dockerImageName} ."
                }
            }
        }
        
        stage('Push Docker image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASS')]) {
                    sh """
                    echo $DOCKERHUB_PASS | docker login -u $DOCKERHUB_USER --password-stdin
                    docker push ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
                    """
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
                      - port: 5000
                        targetPort: 5000
                        nodePort: 30007
                    """

                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml

                    sh("""
                    ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml
                    ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml
                    """)
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline executed successfully!'
        }
        
        failure {
            echo 'Pipeline failed!'
        }
    }
}
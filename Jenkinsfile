pipeline {
    agent any
    tools {
        maven 'Maven'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: 'main']], userRemoteConfigs: [[url: 'https://github.com/DatlaBharath/HelloService-jenkins']]])
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
                    def app = "ratneshpuskar/${env.JOB_NAME.toLowerCase()}:${env.BUILD_NUMBER}"
                    sh """
                        docker build -t ${app} .
                    """
                }
            }
        }
        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh """
                            echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin
                            docker push ratneshpuskar/${env.JOB_NAME.toLowerCase()}:${env.BUILD_NUMBER}
                        """
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh """
                        echo "
                        apiVersion: apps/v1
                        kind: Deployment
                        metadata:
                          name: ${env.JOB_NAME.toLowerCase()}
                        spec:
                          replicas: 1
                          selector:
                            matchLabels:
                              app: ${env.JOB_NAME.toLowerCase()}
                          template:
                            metadata:
                              labels:
                                app: ${env.JOB_NAME.toLowerCase()}
                            spec:
                              containers:
                              - name: ${env.JOB_NAME.toLowerCase()}
                                image: ratneshpuskar/${env.JOB_NAME.toLowerCase()}:${env.BUILD_NUMBER}
                                ports:
                                - containerPort: 5000
                        " > deployment.yaml

                        echo "
                        apiVersion: v1
                        kind: Service
                        metadata:
                          name: ${env.JOB_NAME.toLowerCase()}
                        spec:
                          type: NodePort
                          ports:
                          - port: 5000
                            nodePort: 30007
                          selector:
                            app: ${env.JOB_NAME.toLowerCase()}
                        " > service.yaml

                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml
                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml
                    """
                }
            }
        }
    }
    post {
        success {
            echo 'Deployment was successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
pipeline {
    agent any
    tools {
        maven 'Maven'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git(url: 'https://github.com/DatlaBharath/HelloService-jenkins', branch: 'main')
            }
        }
        
        stage('Build and Package') {
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
            sh '''#!/bin/bash
            echo "$DOCKER_HUB_PASSWORD" | docker login -u "$DOCKER_HUB_USERNAME" --password-stdin
            docker push ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
            '''
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
                      ports:
                        - port: 5000
                          nodePort: 30007
                          protocol: TCP
                      selector:
                        app: helloservice-jenkins
                    """

                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" <<< "${deploymentYaml}"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" <<< "${serviceYaml}"""
                }
            }
        }
    }
    
    post {
        success {
            echo 'Jenkins job completed successfully!'
        }
        failure {
            echo 'Jenkins job failed!'
        }
    }
}
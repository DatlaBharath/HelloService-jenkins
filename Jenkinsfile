pipeline {
    agent any

    tools {
        maven "Maven"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[url: 'https://github.com/DatlaBharath/HelloService-jenkins']]])
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }
        stage('Docker Build and Push') {
            steps {
                script {
                    def imageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                    docker.build(imageName).withRun { c ->
                        def localImage = docker.image(imageName)
                        withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                            sh """
                                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                                docker push ${localImage.id}
                            """
                        }
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    writeFile file: 'deployment.yaml', text: """
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
                    
                    writeFile file: 'service.yaml', text: """
                        apiVersion: v1
                        kind: Service
                        metadata:
                          name: helloservice-jenkins
                        spec:
                          selector:
                            app: helloservice-jenkins
                          ports:
                            - protocol: TCP
                              port: 5000
                              nodePort: 30007
                          type: NodePort
                    """
                    
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
            echo "Job completed successfully."
        }
        failure {
            echo "Job failed. Please check the logs."
        }
    }
}
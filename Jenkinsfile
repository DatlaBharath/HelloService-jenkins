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

        stage('Build and Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def repoName = 'HelloService-jenkins'.toLowerCase()
                    def imageName = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"
                    
                    sh """
                    docker build -t ${imageName} .
                    """
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        def repoName = 'HelloService-jenkins'.toLowerCase()
                        def imageName = "ratneshpuskar/${repoName}:${env.BUILD_NUMBER}"

                        sh """
                        echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
                        docker push ${imageName}
                        """
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
            echo 'Deployment was successful'
        }
        failure {
            echo 'Deployment failed'
        }
    }
}
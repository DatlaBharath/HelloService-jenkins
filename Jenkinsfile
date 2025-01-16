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

        stage('Docker Build and Push') {
            steps {
                script {
                    def imageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKERHUB_PASSWORD', usernameVariable: 'DOCKERHUB_USERNAME')]) {
                        sh """
                            docker build -t ${imageName} .
                            echo ${DOCKERHUB_PASSWORD} | docker login -u ${DOCKERHUB_USERNAME} --password-stdin
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
                      name: hello-service
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
                      selector:
                        app: hello-service
                      ports:
                      - protocol: TCP
                        port: 80
                        targetPort: 5000
                        nodePort: 30007
                      type: NodePort
                    """

                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml

                    sh """
                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@15.206.172.143 "kubectl apply -f -" < deployment.yaml
                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@15.206.172.143 "kubectl apply -f -" < service.yaml
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed.'
        }
    }
}
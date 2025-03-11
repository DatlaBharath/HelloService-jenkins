pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    environment {
        CURL_STATUS = ''
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/DatlaBharath/HelloService-jenkins'
            }
        }
     stage('Curl Request') {
    steps {
        script {
            // Capture the response from the curl request - using sh to execute bash command
            def response = sh(script: 'curl -s http://ec2-13-18-57.ap-south-1.compute.amazonaws.com/app/random-data', returnStdout: true).trim()
            
            // Log the response for debugging
            echo "Curl response: ${response}"
            
            // Escape the response using the same sed approach from GitHub Actions
            def escapedResponse = sh(script: "echo '${response}' | sed 's/\"/\\\\\"/g'", returnStdout: true).trim()
            
            // Send the response to your backend using the same format as GitHub Actions
            sh """
            curl -X POST http://ec2-13-18-57.ap-south-1.compute.amazonaws.com/app/save-curl-response \\
            -H "Content-Type: application/json" \\
            -d "{\\"response\\": \\"${escapedResponse}\\"}"
            """
            
            // Check if the response contains 'success': true
            if (response.contains('"success":true')) {
                echo "Success response received."
                env.CURL_STATUS = 'true'
            } else {
                echo "Failure response received."
                env.CURL_STATUS = 'false'
                error("Curl request failed, terminating pipeline.")
            }
        }
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
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh 'echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin'
                        def imageName = "ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}"
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
                      name: helloservice-deployment
                      labels:
                        app: helloservice
                    spec:
                      replicas: 1
                      selector:
                        matchLabels:
                          app: helloservice
                      template:
                        metadata:
                          labels:
                            app: helloservice
                        spec:
                          containers:
                          - name: helloservice
                            image: ratneshpuskar/helloservice-jenkins:${env.BUILD_NUMBER}
                            ports:
                            - containerPort: 5000
                    """

                    def serviceYaml = """
                    apiVersion: v1
                    kind: Service
                    metadata:
                      name: helloservice-service
                    spec:
                      selector:
                        app: helloservice
                      ports:
                      - protocol: TCP
                        port: 5000
                        targetPort: 5000
                        nodePort: 30007
                      type: NodePort
                    """

                    sh """echo "${deploymentYaml}" > deployment.yaml"""
                    sh """echo "${serviceYaml}" > service.yaml"""

                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < deployment.yaml'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.6.238.137 "kubectl apply -f -" < service.yaml'
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

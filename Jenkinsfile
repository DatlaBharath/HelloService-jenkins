pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    environment {
        PAT = credentials('pat-key')
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
                    def response = sh(script: """
                        curl --location "http://microservice-genai.uksouth.cloudapp.azure.com/api/vmsb/pipelines/initscan" \
                        --header "Content-Type: application/json" \
                        --data '{
                            "encrypted_user_id": "gAAAAABnyCdKTdqwwv1tgbx8CqlTQnyYbqWBATox1Q58q-y8PmXbXc4_65tTO3jRijx92hpZI1juGV-80apcQa0Z72HgzkJsiA==",
                            "scanner_id": 1,
                            "target_branch": "main", 
                            "repo_url": "https://github.com/DatlaBharath/HelloService-jenkins",
                            "pat": "${PAT}"
                        }'
                    """, returnStdout: true).trim()
                    echo "Curl response: ${response}"
                    
                    def escapedResponse = sh(script: "echo '${response}' | sed 's/\"/\\\\\"/g'", returnStdout: true).trim()
                    def jsonData = "{\"response\": \"${escapedResponse}\"}"
                    def contentLength = jsonData.length()
                    
                    sh """
                    curl -X POST http://ec2-13-201-18-57.ap-south-1.compute.amazonaws.com/app/save-curl-response-jenkins?sessionId=adminEC23C9F6-77AD-9E64-7C02-A41EF19C7CC3 \
                    -H "Content-Type: application/json" \
                    -H "Content-Length: ${contentLength}" \
                    -d '${jsonData}'
                    """
                    
                    def total_vulnerabilities = sh(script: "echo '${response}' | jq -r '.total_vulnerabilites'", returnStdout: true).trim()
                    def high = sh(script: "echo '${response}' | jq -r '.high'", returnStdout: true).trim()
                    def medium = sh(script: "echo '${response}' | jq -r '.medium'", returnStdout: true).trim()

                    try {
                        total_vulnerabilities = total_vulnerabilities.toInteger()
                        high = high.toInteger()
                        medium = medium.toInteger()
                    } catch (Exception e) {
                        echo "Warning: Could not parse total_vulnerabilities as integer: ${total_vulnerabilities}"
                        total_vulnerabilities = -1
                    }

                    if (high + medium <= 0) {
                        echo "Success: No high and medium vulnerabilities found."
                        env.CURL_STATUS = 'true'
                    } else {
                        echo "Failure: Found ${total_vulnerabilities} vulnerabilities."
                        env.CURL_STATUS = 'false'
                        error("Vulnerabilities found, terminating pipeline.")
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
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@52.66.205.189 "kubectl apply -f -" < deployment.yaml'
                    sh 'ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@52.66.205.189 "kubectl apply -f -" < service.yaml'
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
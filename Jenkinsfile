pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM', 
                    branches: [[name: '*/main']], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [], 
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: 'https://github.com/DatlaBharath/HelloService-jenkins']]
                ])
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
                    def imageName = "ratneshpuskar/${env.JOB_NAME.toLowerCase()}:${env.BUILD_NUMBER}"
                    sh "docker build -t ${imageName} ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                    sh """
                        echo ${DOCKERHUB_PASSWORD} | docker login -u ${DOCKERHUB_USER} --password-stdin
                        docker push ratneshpuskar/${env.JOB_NAME.toLowerCase()}:${env.BUILD_NUMBER}
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
                    """

                    def serviceYaml = """
                        apiVersion: v1
                        kind: Service
                        metadata:
                          name: ${env.JOB_NAME.toLowerCase()}
                        spec:
                          type: NodePort
                          selector:
                            app: ${env.JOB_NAME.toLowerCase()}
                          ports:
                          - protocol: TCP
                            port: 5000
                            targetPort: 5000
                            nodePort: 30007
                    """
                    sh """echo '${deploymentYaml}' > deployment.yaml"""
                    sh """echo '${serviceYaml}' > service.yaml"""

                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.110.102.129 "kubectl apply -f -" < deployment.yaml"""
                    sh """ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@3.110.102.129 "kubectl apply -f -" < service.yaml"""
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment was successful.'
        }
        failure {
            echo 'Deployment failed.'
        }
    }
}
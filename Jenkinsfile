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
 
        stage('Build Docker Image') {

            steps {

                script {

                    def imageName = "ratneshpushkar/helloservice-jenkins:${env.BUILD_NUMBER}"

                    sh "docker build -t ${imageName} ."

                }

            }

        }
 
        stage('Push Docker Image') {

            steps {

                script {

                    withCredentials([usernamePassword(credentialsId: 'docker_cred', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {

                        sh 'echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin'     

                        def imageName = "ratneshpushkar/helloservice-jenkins:${env.BUILD_NUMBER}"

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

                      name: helloservice-jenkins

                      labels:

                        app: helloservice

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

                            image: ratneshpushkar/helloservice-jenkins:${env.BUILD_NUMBER}

                            ports:

                            - containerPort: 5000

                    """
 
                    def serviceYaml = """

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
 
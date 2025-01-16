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
                    withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh """
                        docker build -t ${imageName} .
                        echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
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
                        - protocol: TCP
                          port: 5000
                          nodePort: 30007
                    """

                    sh """
                    echo '${deploymentYaml}' | ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.235.113.19 "kubectl apply -f -"
                    echo '${serviceYaml}' | ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.235.113.19 "kubectl apply -f -"
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Job succeeded'
        }
        failure {
            echo 'Job failed'
        }
    }
}
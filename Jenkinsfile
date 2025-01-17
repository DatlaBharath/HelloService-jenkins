pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: '', url: 'https://github.com/DatlaBharath/HelloService-jenkins'
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
                    def repoName = 'ratneshpuskar/hello_service-jenkins'
                    def tagName = "${env.BUILD_NUMBER}"
                    sh """
                        docker build -t ${repoName}:${tagName} .
                    """
                }
            }
        }
        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub_credentials', passwordVariable: 'DOCKER_HUB_PASS', usernameVariable: 'DOCKER_HUB_USER')]) {
                    script {
                        sh """
                            echo "${DOCKER_HUB_PASS}" | docker login -u "${DOCKER_HUB_USER}" --password-stdin
                            docker push ratneshpuskar/hello_service-jenkins:${env.BUILD_NUMBER}
                        """
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def deploymentYaml = '''
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
        image: ratneshpuskar/hello_service-jenkins:${env.BUILD_NUMBER}
        ports:
        - containerPort: 5000
'''

                    def serviceYaml = '''
apiVersion: v1
kind: Service
metadata:
  name: hello-service
spec:
  selector:
    app: hello-service
  ports:
    - protocol: TCP
      port: 5000
      targetPort: 5000
      nodePort: 30007
  type: NodePort
'''

                    writeFile file: 'deployment.yaml', text: deploymentYaml
                    writeFile file: 'service.yaml', text: serviceYaml

                    sh """
                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.234.112.157 "kubectl apply -f -" < deployment.yaml
                        ssh -i /var/test.pem -o StrictHostKeyChecking=no ubuntu@13.234.112.157 "kubectl apply -f -" < service.yaml
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'The pipeline has been completed successfully.'
        }
        failure {
            echo 'The pipeline has failed.'
        }
    }
}
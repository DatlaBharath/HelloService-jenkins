pipeline {
    agent any

    environment {
        dockerImage = ''
        registryCredential = 'dockerhub-credentials'
        registry = 'yourDockerRegistry'
        kubernetesHost = 'user@kubernetesHost'
        deploymentName = 'app-deployment'
        containerName = 'app-container'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/user/repository.git'
            }
        }
        
        stage('Build') {
            steps {
                script {
                    sh 'mvn clean package'
                }
            }
        }
        
        stage('Docker: Build Image') {
            steps {
                script {
                    dockerImage = docker.build("${registry}:${env.BUILD_ID}")
                }
            }
        }
        
        stage('Docker: Push Image') {
            steps {
                script {
                    docker.withRegistry("https://${registry}", 'dockerhub-credentials') {
                        dockerImage.push()
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'ssh-credentials', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh """
                            sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no $USER@$kubernetesHost << EOF
                            kubectl set image deployment/${deploymentName} ${containerName}=${registry}:${env.BUILD_ID}
                            EOF
                        """
                    }
                }
            }
        }
    }
}
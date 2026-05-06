pipeline {
    agent any

    stages {

        stage('Pull Docker Image') {
            steps {
                sh 'docker pull ganpatsingh05/devops-app'
            }
        }

        stage('Stop Old Container') {
            steps {
                sh '''
                docker stop devops-container || true
                docker rm devops-container || true
                '''
            }
        }

        stage('Run New Container') {
            steps {
                sh 'docker run -d -p 8087:8080 --name devops-container ganpatsingh05/devops-app'
            }
        }

    }
}
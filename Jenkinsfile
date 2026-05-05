pipeline {
    agent any

    stages {

        stage('Pull Docker Image') {
            steps {
                sh 'docker pull ganpatsingh05/devops-app'
            }
        }

        stage('Run Container') {
            steps {
                sh 'docker run -d -p 8087:8080 ganpatsingh05/devops-app'
            }
        }

    }
}
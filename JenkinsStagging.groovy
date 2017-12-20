pipeline {
    agent {
        label 'docker01'
    }
    options {
        ansiColor colorMapName: 'XTerm'
    }
    parameters {
        string(
            name: 'APP_SERVERS',
            defaultValue: '10.10.10.122',
            description: 'Deploy container to these servers. List of servers separated by comma.'
        )
    }
    stages {
        stage('Build') {
            steps {
                sh "docker build --rm=true -t captain-hook-staging ."
            }
        }

        stage('Prepare and upload to registry ') {
            steps {
                withCredentials([string(credentialsId: 'docker-registry-azure', variable: 'DRpass')]) {
                    sh 'docker login roihunter.azurecr.io -u roihunter -p "$DRpass"'
                    sh "docker tag captain-hook-staging roihunter.azurecr.io/captain-hook/staging"
                    sh "docker push roihunter.azurecr.io/captain-hook/staging"
                    sh "docker rmi captain-hook-staging"
                    sh "docker rmi roihunter.azurecr.io/captain-hook/staging"
                }
            }
        }

        stage('Deploy API container') {
            steps {
                withCredentials([
                    string(credentialsId: 'docker-registry-azure', variable: 'DRpass')
                ]) {
                    script {
                        def servers = params.APP_SERVERS.tokenize(',')

                        for (item in servers) {
                            sshagent(['5de2256c-107d-4e4a-a31e-2f33077619fe']) {
                                sh """ssh -oStrictHostKeyChecking=no -t -t jenkins@${item} <<EOF
                                docker login roihunter.azurecr.io -u roihunter -p "$DRpass"
                                docker pull roihunter.azurecr.io/captain-hook/staging
                                docker stop captain-hook-staging
                                docker rm -v captain-hook-staging
                                docker run --detach -p 8006:8005 \
                                    --hostname=captain-hook-staging-"$item" \
                                    --name=captain-hook-staging \
                                    --restart=always \
                                    roihunter.azurecr.io/captain-hook/staging
                                exit
                                EOF"""
                            }
                        }
                    }
                }
            }
        }
        stage('Send notification') {
            steps {
                withCredentials([string(credentialsId: 'slack-bot-token', variable: 'slackToken')]) {
                    slackSend channel: 'deploy', message: 'Captain Hook staging deployed', color: '#4280f4', token: slackToken, botUser: true
                }
            }
        }
    }
    post {
        always {
            // Clean Workspace
            cleanWs()
        }
    }
}

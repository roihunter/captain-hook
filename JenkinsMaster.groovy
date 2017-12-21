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
        booleanParam(
            name: "BUILD_IMAGE",
            defaultValue: true,
            description: "Build image and upload it to Docker registry"
        )
    }
    stages {
        when {
            expression {
                return params.BUILD_IMAGE
            }
        }
        stage('Build') {
            steps {
                sh "docker build --rm=true -t captain-hook-master ."
            }
        }

        stage('Prepare and upload to registry ') {
            when {
                expression {
                    return params.BUILD_IMAGE
                }
            }
            steps {
                withCredentials([string(credentialsId: 'docker-registry-azure', variable: 'DRpass')]) {
                    sh 'docker login roihunter.azurecr.io -u roihunter -p "$DRpass"'
                    sh "docker tag captain-hook-master roihunter.azurecr.io/captain-hook/master"
                    sh "docker push roihunter.azurecr.io/captain-hook/master"
                    sh "docker rmi captain-hook-master"
                    sh "docker rmi roihunter.azurecr.io/captain-hook/master"
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
                                docker pull roihunter.azurecr.io/captain-hook/master
                                docker stop captain-hook-master
                                docker rm -v captain-hook-master
                                docker run --detach -p 8007:8005 \
                                    --hostname=captain-hook-master-"$item" \
                                    --name=captain-hook-master \
                                    --restart=always \
                                    roihunter.azurecr.io/captain-hook/master
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
                    slackSend channel: 'deploy', message: 'Captain Hook master deployed', color: '#4280f4', token: slackToken, botUser: true
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

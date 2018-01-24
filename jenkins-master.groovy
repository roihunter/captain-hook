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
        string(
            name: 'RABBIT_HOST',
            defaultValue: '10.10.10.96',
            description: 'RabbitMQ server'
        )
        string(
            name: 'RABBIT_PORT',
            defaultValue: '5672',
            description: 'RabbitMQ port'
        )
    }
    stages {
        stage('Build') {
            when {
                expression {
                    return params.BUILD_IMAGE
                }
            }
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
                    string(credentialsId: 'docker-registry-azure', variable: 'DRpass'),
                    string(credentialsId: "captainhook-master-facebook-verify-token", variable: "captainhook_facebook_verify_token" ),
                    usernamePassword(
                        credentialsId: "captainhook-master-rabbit-username-password",
                        passwordVariable: "captainhook_rabbit_password",
                        usernameVariable: "captainhook_rabbit_username"
                    )   
                ]) {
                    script {
                        def servers = params.APP_SERVERS.tokenize(',')

                        for (item in servers) {
                            sshagent(['5de2256c-107d-4e4a-a31e-2f33077619fe']) {
                                sh """ssh -oStrictHostKeyChecking=no -t -t jenkins@${item} <<EOF
                                docker login roihunter.azurecr.io -u roihunter -p "$DRpass"
                                docker pull roihunter.azurecr.io/captain-hook/master
                                docker stop captain-hook-master; true
                                docker rm -v captain-hook-master; true
                                docker run --detach -p 8007:8005 \
                                    -e "GUNICORN_BIND=0.0.0.0:8005" \
                                    -e "GUNICORN_WORKERS=4" \
                                    -e "CAPTAINHOOK_PROFILE=master" \
                                    -e "CAPTAINHOOK_FACEBOOK_VERIFY_TOKEN=${captainhook_facebook_verify_token}" \
                                    -e "CAPTAINHOOK_RABBIT_LOGIN=${captainhook_rabbit_username}" \
                                    -e "CAPTAINHOOK_RABBIT_PASSWORD=${captainhook_rabbit_password}" \
                                    -e "CAPTAINHOOK_RABBIT_HOST=${params.RABBIT_HOST}" \
                                    -e "CAPTAINHOOK_RABBIT_PORT=${params.RABBIT_PORT}" \
                                    --hostname=captain-hook-master-${item} \
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

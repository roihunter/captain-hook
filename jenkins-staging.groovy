pipeline {
    agent {
        label 'docker01'
    }

    options {
        ansiColor colorMapName: 'XTerm'
    }

    parameters {
        booleanParam(
            name: "BUILD_IMAGE",
            defaultValue: true,
            description: "Build image and upload it to Docker registry"
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
                sh "docker build --rm=true -t roihunter.azurecr.io/captain-hook/staging ."

                withCredentials([string(credentialsId: 'docker-registry-azure', variable: 'DRpass')]) {
                    sh 'docker login roihunter.azurecr.io -u roihunter -p "$DRpass"'
                    sh("""
                        for tag in $BUILD_NUMBER latest; do
                            docker tag roihunter.azurecr.io/captain-hook/staging roihunter.azurecr.io/captain-hook/staging:\${tag}
                            docker push roihunter.azurecr.io/captain-hook/staging:\${tag}
                            docker rmi roihunter.azurecr.io/captain-hook/staging:\${tag}
                        done
                    """)
                }
            }
        }
        stage('Deploy API container') {
            environment {
                IMAGE_TAG = getImageTag(params.BUILD_IMAGE)
            }

            steps {
                script {
                    kubernetesDeploy(
                        configs: '**/kubernetes/captain-hook-staging-deployment.yaml,**/kubernetes/captain-hook-staging-service.yaml',
                        credentialsType: 'SSH',
                        dockerCredentials: [
                            [credentialsId: 'docker-azure-credentials', url: 'http://roihunter.azurecr.io']
                        ],
                        kubeConfig: [path: ''],
                        secretName: 'regsecret',
                        ssh: [
                            sshCredentialsId: 'daece77c-7b7f-48e2-b7c8-f79982004c33',
                            sshServer: '94.130.216.247'
                        ],
                        textCredentials: [
                            certificateAuthorityData: '',
                            clientCertificateData: '',
                            clientKeyData: '',
                            serverUrl: 'https://'
                        ]
                    )
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

String getImageTag(Boolean build_image) {
    if (build_image) {
        return env.BUILD_NUMBER
    } else {
        return "latest"
    }
}

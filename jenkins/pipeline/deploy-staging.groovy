pipeline {
    agent {
        label 'docker01'
    }

    options {
        ansiColor colorMapName: 'XTerm'
    }

    parameters {
        booleanParam(
            name: 'SEND_SLACK_NOTIFICATION',
            defaultValue: true,
            description: 'Send notification about deploy to Slack.'
        )
    }

    environment {
        // real branch is "dev", but we use it as "staging"
        BRANCH_NAME = "staging"
    }

    stages {
        stage("Build Docker image") {
            steps {
                script {
                    def rootDir = pwd()
                    def build = load "${rootDir}/jenkins/pipeline/_build.groovy"
                    build.buildDockerImage(env.BRANCH_NAME)
                }
            }
        }

        stage('Deploy API to Kubernetes') {
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

        stage('Send Slack notification') {
            when {
                expression {
                    return params.SEND_SLACK_NOTIFICATION
                }
            }
            steps {
                script {
                    def rootDir = pwd()
                    def utils = load "${rootDir}/jenkins/pipeline/_utils.groovy"
                    utils.sendSlackNotification(env.BRANCH_NAME, '#D39D47')
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

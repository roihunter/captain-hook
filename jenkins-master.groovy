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
            defaultValue: '35.233.65.128',
            description: 'RabbitMQ server'
        )
        string(
            name: 'RABBIT_PORT',
            defaultValue: '5672',
            description: 'RabbitMQ port'
        )
        booleanParam(
            name: 'SEND_SLACK_NOTIFICATION',
            defaultValue: true,
            description: 'Send notification about deploy to Slack.'
        )
    }

    environment {
        BRANCH_NAME = env.GIT_BRANCH.replaceFirst("origin/", "")
    }

    stages {
        stage("Build Docker image") {
            when {
                expression {
                    return params.BUILD_IMAGE
                }
            }
            steps {
                script {
                    def rootDir = pwd()
                    def build = load "${rootDir}/jenkins/pipeline/_build.groovy"
                    build.buildDockerImage(env.BRANCH_NAME)
                }
            }
        }

        stage('Deploy API container') {
            steps {
                withCredentials([file(credentialsId: 'jenkins-roihunter-master-kubeconfig', variable: 'kube_config')]) {
                    kubernetesDeploy(
                        configs: '**/kubernetes/captain-hook-master-deployment.yaml,**/kubernetes/captain-hook-master-service.yaml',
                        dockerCredentials: [
                             [credentialsId: 'docker-azure-credentials', url: 'http://roihunter.azurecr.io']
                        ],
                        kubeConfig: [
                            path: "$kube_config"
                        ],
                        secretName: 'regsecret',
                        ssh: [
                            sshCredentialsId: '*',
                            sshServer: ''
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
                    utils.sendSlackNotification(env.BRANCH_NAME, '#0E8A16')
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

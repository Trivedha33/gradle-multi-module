pipeline {
    agent any

    environment {
        GIT_STRATEGY = 'clone'
        GRADLE_LOG_OPT = '-Dorg.gradle.logging.level=info'
        NO_COMPOSITE_BUILD_OPT = '-Dno-composite-build'
        GRADLE_OPTS = "${GRADLE_LOG_OPT} ${NO_COMPOSITE_BUILD_OPT}"
        JAVA_HOME = 'C:\\java\\jdk11'
    }

    stages {
        stage('Build') {
            when {
                anyOf {
                    changeRequest()
                    expression { return env.CI_PIPELINE_SOURCE == 'push' }
                }
            }
            steps {
                sh './gradlew build downloadJavaAgent jacocoTestReport'
            }
            post {
                success {
                    archiveArtifacts artifacts: '.gradle/,build/libs/*.war,build/opentelemetry', allowEmptyArchive: true
                }
            }
        }

        stage('Tag for Release') {
            when {
                anyOf {
                    branch 'master'
                    branch pattern: 'release-[0-9]+\\.[0-9]+', comparator: 'REGEXP'
                }
            }
            steps {
                sh './gradlew tagForRelease'
            }
        }

        stage('Publish to Artifactory') {
            when {
                expression { return env.CI_COMMIT_TAG != null }
            }
            steps {
                sh './gradlew publish'
            }
        }

        stage('Publish Docker Image') {
            agent {
                docker {
                    image 'artifactory.prod.tableautools.com:6555/tableau/container-services/octopus-shared-code:0.2.170'
                    label 'nerv-aws-docker-linux'
                }
            }
            when {
                branch 'master'
            }
            steps {
                sh './scripts/build_docker_image.sh'
            }
        }

        stage('Publish Octopus Release') {
            when {
                branch 'master'
            }
            steps {
                sh './scripts/create_release.sh'
            }
        }

        stage('Twistlock Scan') {
            when {
                anyOf {
                    branch 'master'
                    branch pattern: 'release-[0-9]+\\.[0-9]+', comparator: 'REGEXP'
                }
            }
            steps {
                sh './scripts/twistlock_scan.sh'
            }
        }

        stage('Generate Javadoc') {
            when {
                branch 'master'
            }
            steps {
                sh './gradlew javadoc'
                sh 'mv build/docs/javadoc/ public/'
                archiveArtifacts artifacts: 'public/**/*', allowEmptyArchive: true
            }
        }

        stage('Deploy to Sandbox') {
            when {
                branch 'master'
            }
            steps {
                sh './gradlew build'
                sh './gradlew deployToSandbox'
            }
        }

        stage('Push Blame Info') {
            when {
                branch 'master'
            }
            steps {
                sh './scripts/push_blame_info.sh'
            }
        }

        stage('Resource Group') {
            steps {
                script {
                    def servers = 1
                    def resourceGroup = env.CI_PROJECT_ID ? 
                        "${env.CI_PROJECT_ID}-${env.BUILD_NUMBER.toInteger() % servers}" : 
                        'default-group'
                    
                    writeFile file: 'resource-group.env', text: "RESOURCE_GROUP=${resourceGroup}"
                }
            }
        }

        stage('Manual Deploy') {
            when {
                expression { return env.CI_PIPELINE_SOURCE == 'schedule' }
            }
            steps {
                build job: 'gitlab-tableau-server',
                      parameters: [
                          string(name: 'PARENT_PIPELINE_ID', value: env.BUILD_ID),
                          string(name: 'RESOURCE_GROUP', value: readFile('resource-group.env').trim())
                      ]
            }
        }

        stage('Beta Artifacts Publish') {
            when {
                branch pattern: '.*'
            }
            steps {
                sh './gradlew publish -PpublishBetaArtifactsFromTaskBranch=true'
                sh 'git tag v4.209.67'
                sh './scripts/build_docker_image.sh'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}


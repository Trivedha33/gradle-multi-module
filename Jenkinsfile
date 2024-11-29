pipeline {
    agent any

    environment {
        GIT_STRATEGY = 'clone'
        GRADLE_LOG_OPT = '-Dorg.gradle.logging.level=info'
        NO_COMPOSITE_BUILD_OPT = '-Dno-composite-build'
        GRADLE_OPTS = "${GRADLE_LOG_OPT} ${NO_COMPOSITE_BUILD_OPT}"
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    }

    stages {
        stage('Build') {
            agent {
                docker {
                    image 'artifactory.prod.tableautools.com:6555/tableau/nerv/openjdk-docker-image:2.30.0-11'
                    args '-u root:root'
                }
            }
            steps {
                sh './gradlew build downloadJavaAgent jacocoTestReport'
                archiveArtifacts artifacts: '.gradle/**, build/libs/*.war, build/opentelemetry/**', allowEmptyArchive: true
            }
        }

        stage('Tag for Release') {
            when {
                branch 'main'
            }
            steps {
                sh './gradlew tagForRelease'
            }
        }

        stage('Publish to Artifactory') {
            when {
                expression { return env.GIT_TAG != null }
            }
            steps {
                sh './gradlew publish'
            }
        }

        stage('Publish Docker Image') {
            agent {
                docker {
                    image 'artifactory.prod.tableautools.com:6555/tableau/container-services/octopus-shared-code:0.2.170'
                    args '-u root:root'
                }
            }
            when {
                branch 'main'
            }
            steps {
                sh './scripts/build_docker_image.sh'
            }
        }

        stage('Publish Octopus Release') {
            agent {
                docker {
                    image 'artifactory.prod.tableautools.com:6555/tableau/container-services/octopus-shared-code:0.2.170'
                    args '-u root:root'
                }
            }
            when {
                branch 'main'
            }
            steps {
                sh './scripts/create_release.sh'
            }
        }

        stage('Twistlock Scan') {
            when {
                branch 'main'
            }
            steps {
                sh './scripts/twistlock_scan.sh'
            }
        }

        stage('Generate and Publish Javadoc') {
            when {
                branch 'main'
            }
            steps {
                sh './gradlew javadoc'
                sh 'mv build/docs/javadoc/ public/'
                archiveArtifacts artifacts: 'public/**', allowEmptyArchive: true
            }
        }

        stage('Deploy to Sandbox') {
            when {
                branch 'main'
            }
            steps {
                sh './gradlew build'
                sh './gradlew deployToSandbox'
            }
        }

        stage('Push Blame Info') {
            when {
                branch 'main'
            }
            steps {
                sh './scripts/push_blame_info.sh'
            }
        }

        stage('Resource Group') {
            steps {
                script {
                    def servers = 1
                    def resourceGroup = "${env.JOB_NAME}-${env.BUILD_NUMBER % servers}"
                    writeFile file: 'resource-group.env', text: "RESOURCE_GROUP=${resourceGroup}"
                    archiveArtifacts artifacts: 'resource-group.env', allowEmptyArchive: true
                }
            }
        }

        stage('Deploy') {
            steps {
                build job: 'gitlab-tableau-server', parameters: [
                    string(name: 'PARENT_PIPELINE_ID', value: "${env.BUILD_ID}"),
                    string(name: 'RESOURCE_GROUP', value: "${env.RESOURCE_GROUP}")
                ]
            }
        }

        stage('Publish to Beta') {
            when {
                branch pattern: '.*', comparator: 'REGEXP'
            }
            steps {
                sh './gradlew publish -PpublishBetaArtifactsFromTaskBranch=true'
                sh 'git tag v4.209.67'
                sh './scripts/build_docker_image.sh'
            }
        }
    }
}
pipeline {
    agent any

    environment {
        GIT_STRATEGY = 'clone'
        GRADLE_LOG_OPT = '-Dorg.gradle.logging.level=info'
        NO_COMPOSITE_BUILD_OPT = '-Dno-composite-build'
        GRADLE_OPTS = "${GRADLE_LOG_OPT} ${NO_COMPOSITE_BUILD_OPT}"
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
        GIT_STRATEGY = 'clone'
        GRADLE_LOG_OPT = '-Dorg.gradle.logging.level=info'
        NO_COMPOSITE_BUILD_OPT = '-Dno-composite-build'
        GRADLE_OPTS = '${GRADLE_LOG_OPT} ${NO_COMPOSITE_BUILD_OPT}'
    }

    stages {
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

        stage('linux build') {
            steps {
                sh '''./gradlew build downloadJavaAgent jacocoTestReport'''
            }
        }
        stage('tag for release') {
            steps {
                sh '''./gradlew tagForRelease'''
            }
        }
        stage('publish to artifactory') {
            steps {
                sh '''./gradlew publish'''
            }
        }
        stage('publish docker image') {
            steps {
                sh '''./scripts/build_docker_image.sh'''
            }
        }
        stage('publish octopus release') {
            steps {
                sh '''./scripts/create_release.sh'''
            }
        }
        stage('twistlock scan') {
            steps {
                sh '''./scripts/twistlock_scan.sh'''
            }
        }
        stage('pages') {
            steps {
                sh '''./gradlew javadoc'''
                sh '''mv build/docs/javadoc/ public/'''
            }
        }
        stage('deploy to sandbox') {
            steps {
                sh '''./gradlew build'''
                sh '''./gradlew deployToSandbox'''
            }
        }
        stage('push blame info') {
            steps {
                sh '''./scripts/push_blame_info.sh'''
            }
        }
        stage('resource group') {
            steps {
                sh '''SERVERS=1
RESOURCE_GROUP="${CI_PROJECT_ID}-${CI_PIPELINE_ID % $SERVERS}"
echo "RESOURCE_GROUP=$RESOURCE_GROUP" >> resource-group.env
'''
            }
        }
        stage('publish-to-beta') {
            steps {
                sh '''./gradlew publish -PpublishBetaArtifactsFromTaskBranch=true'''
                sh '''git tag v4.209.67'''
                sh '''./scripts/build_docker_image.sh'''
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
    }
}

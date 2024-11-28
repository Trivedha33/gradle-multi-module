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
        stage('Build') {
            agent { docker { image 'artifactory.prod.tableautools.com:6555/tableau/nerv/openjdk-docker-image:2.30.0-11' args '-u root:root' } }
            steps {
                ./gradlew build downloadJavaAgent jacocoTestReport
                archiveArtifacts artifacts: '.gradle/**, build/libs/*.war, build/opentelemetry/**', allowEmptyArchive: true
            }
        }
        stage('Tag for Release') {
            when { branch 'main' }
            steps {
                ./gradlew tagForRelease
            }
        }
        stage('Publish Docker Image') {
            agent { docker { image 'artifactory.prod.tableautools.com:6555/tableau/container-services/octopus-shared-code:0.2.170' args '-u root:root' } }
            when { branch 'main' }
            steps {
                ./scripts/build_docker_image.sh
            }
        }
    }
}

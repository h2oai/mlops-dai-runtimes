#!/usr/bin/groovy

@Library('test-shared-library@1.17') _

import ai.h2o.ci.Utils

JAVA_IMAGE = 'nimmis/java-centos:openjdk-8-jdk'
DOCKER_JAVA_HOME = '/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64'
NODE_LABEL = 'master'

def VERSION = null
def utilsLib = new Utils()

pipeline {
    agent none // specify agent on a per stage basis

    // Setup job options
    options {
        ansiColor('xterm')
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Init') {
            agent { label NODE_LABEL }
            steps {
                script {
                    deleteDir()
                    checkout scm
                }
            }
        }

        stage('Test') {
            agent {
                docker { // run inside JAVA_IMAGE container on NODE_LABEL host
                    image JAVA_IMAGE
                    label NODE_LABEL
                }
            }
            steps {
                script {
                    VERSION = getVersion()
                    echo "Version: ${VERSION}"
                    sh "JAVA_HOME=${DOCKER_JAVA_HOME} ./gradlew check"
                }
            }
            post {
                always {
                    testReport 'aws-lambda-scorer/lambda-template/build/reports/tests/test', 'JUnit tests'
                }
            }
        }

        stage('Build') {
            agent {
                docker { // run inside JAVA_IMAGE container on NODE_LABEL host
                    image JAVA_IMAGE
                    label NODE_LABEL
                }
            }
            steps {
                script {
                    sh "JAVA_HOME=${DOCKER_JAVA_HOME} ./gradlew distributionZip"
                    if (isRelease(VERSION)) {
                        utilsLib.appendBuildDescription("Release ${VERSION}")
                    }
                }
            }
            post {
                success {
                    arch "build/dai-deployment-templates-${VERSION}.zip"
                }
            }
        }

        stage('Publish to S3') {
            agent { label NODE_LABEL } // run on host
            when {
                expression {
                    return doPublish()
                }
            }
            steps {
                script {
                    s3upDocker {
                        localArtifact = "build/dai-deployment-templates-${VERSION}.zip"
                        artifactId = 'dai-deployment-template'
                        version = VERSION
                        keepPrivate = true
                        isRelease = isRelease(VERSION)
                        platform = "any"
                    }
                }
            }
        }
    }
}

/**
 * @param version version to test
 * @return true, if given version does not contain SNAPSHOT.
 */
def isRelease(version) {
    echo version
    return !version.contains('SNAPSHOT')
}

/**
 * @return version specified in gradle.properties
 */
def getVersion() {
    def version = sh(
            script: "JAVA_HOME=${DOCKER_JAVA_HOME} ./gradlew -q -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false printVersion",
            returnStdout: true).trim()
    if (!version) {
        error "Version must be set"
    }
    return version
}

/**
 * @return true, if we are building master or rel-* branch
 */
def doPublish() {
    return  true
    //return env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith("rel-")
}

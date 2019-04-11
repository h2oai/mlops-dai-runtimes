#!/usr/bin/groovy

@Library('test-shared-library@1.17') _

import ai.h2o.ci.Utils

JAVA_IMAGE = 'nimmis/java-centos:openjdk-8-jdk'
DOCKER_JAVA_HOME = '/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64'
NODE_LABEL = 'master'

def VERSION = null
def utilsLib = new Utils()

pipeline {
    // Specify agent on a per stage basis.
    agent none

    // Setup job options.
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
            // Run inside JAVA_IMAGE container on NODE_LABEL host.
            agent {
                docker {
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
                    testReport 'common/transform/build/reports/tests/test', 'JUnit tests: common/transform'
                }
            }
        }

        stage('Build') {
            // Run inside JAVA_IMAGE container on NODE_LABEL host.
            agent {
                docker {
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
            // Run on NODE_LABEL host.
            agent { label NODE_LABEL }
            when {
                expression {
                    return isReleaseBranch() || isMasterBranch()
                }
            }
            steps {
                script {
                    s3upDocker {
                        localArtifact = "build/dai-deployment-templates-${VERSION}.zip"
                        artifactId = 'dai-deployment-templates'
                        version = VERSION
                        keepPrivate = false
                        isRelease = isRelease()
                        platform = "any"
                    }
                }
            }
        }
    }
}

/**
 * @return Version specified in gradle.properties. Fails if master contains a release version (to prevent pushing
 * release version accidentally).
 */
def getVersion() {
    def version = sh(
            script: "JAVA_HOME=${DOCKER_JAVA_HOME} ./gradlew -q -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false printVersion",
            returnStdout: true).trim()
    if (!version) {
        error "Version must be set"
    }
    if (isMasterBranch() && !isSnapshotVersion(version)) {
        error "Master contains a non-snapshot version"
    }
    return version
}

/**
 * @return True, if the given version string denotes a snapshot version.
 */
def isSnapshotVersion(version) {
    return version.endsWith("-SNAPSHOT")
}

/**
 * @return True, if we are on the master branch.
 */
def isMasterBranch() {
    return env.BRANCH_NAME == "master"
}

/**
 * @return True, if we are on a release branch.
 */
def isReleaseBranch() {
    return env.BRANCH_NAME.startsWith("release")
}

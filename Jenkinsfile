#!/usr/bin/groovy

@Library('test-shared-library@1.19') _

import ai.h2o.ci.Utils

JAVA_IMAGE = 'openjdk:8u222-jdk-slim'
NODE_LABEL = 'docker'

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

    parameters {
        booleanParam(
            name: 'PUSH_TO_HARBOR',
            defaultValue: false,
            description: 'Whether to also push Docker images to Harbor.',
        )
        booleanParam(
            name: 'PUSH_TO_DOCKERHUB',
            defaultValue: false,
            description: 'Whether to also push Docker images to DockerHub.',
        )
        booleanParam(
            name: 'PUSH_DISTRIBUTION_ZIP',
            defaultValue: false,
            description: 'Whether to also push distribution ZIP archive to S3.',
        )
    }

    stages {

        stage('1. Init') {
            agent { label NODE_LABEL }
            steps {
                script {
                    deleteDir()
                    checkout scm
                }
            }
        }

        stage('2. Test') {
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
                    sh "./gradlew check"
                }
            }
            post {
                always {
                    testReport 'common/transform/build/reports/tests/test', 'JUnit tests: common/transform'
                }
            }
        }

        stage('3. Build') {
            // Run inside JAVA_IMAGE container on NODE_LABEL host.
            agent {
                docker {
                    image JAVA_IMAGE
                    label NODE_LABEL
                }
            }
            steps {
                script {
                    sh "./gradlew distributionZip"
                    if (isReleaseVersion(VERSION)) {
                        utilsLib.appendBuildDescription("Release ${VERSION}")
                    }
                }
            }
            post {
                success {
                    arch "build/dai-deployment-templates-${VERSION}.zip"
                    stash name: "distribution-zip", includes: "build/dai-deployment-templates-${VERSION}.zip"
                }
            }
        }

        stage('4. Publish to S3') {
            // Run on NODE_LABEL host.
            agent { label NODE_LABEL }
            when {
                expression {
                    return isReleaseBranch() || isMasterBranch() || params.PUSH_DISTRIBUTION_ZIP
                }
            }
            steps {
                script {
                    unstash name: "distribution-zip"
                    s3upDocker {
                        localArtifact = "build/dai-deployment-templates-${VERSION}.zip"
                        artifactId = 'dai-deployment-templates'
                        version = VERSION
                        keepPrivate = false
                        isRelease = isReleaseVersion(VERSION)
                        platform = "any"
                    }
                }
            }
            post {
                success {
                    script {
                        echo "Keep this build.."
                        currentBuild.setKeepLog(true)
                    }
                }
            }
        }

        stage('5. Push Docker Images To Harbor') {
            when {
                expression {
                    return isReleaseBranch() || isMasterBranch() || params.PUSH_TO_HARBOR
                }
            }
            agent {
                docker {
                    image JAVA_IMAGE
                    label NODE_LABEL
                }
            }
            steps {
                script {
                    def gitCommitHash = env.GIT_COMMIT
                    def imageTags = "${VERSION},${gitCommitHash}"
                    withDockerCredentials("harbor.h2o.ai") {
                        sh "./gradlew jib \
                            -Djib.to.auth.username=${DOCKER_USERNAME} \
                            -Djib.to.auth.password=${DOCKER_PASSWORD} \
                            -Djib.to.tags=${imageTags} \
                            -Djib.allowInsecureRegistries=true \
                            -DsendCredentialsOverHttp=true"
                    }
                }
            }
        }

        stage('6. Push Docker Images To DockerHub') {
            when {
                expression {
                    return isReleaseBranch() || params.PUSH_TO_DOCKERHUB
                }
            }
            agent {
                docker {
                    image JAVA_IMAGE
                    label NODE_LABEL
                }
            }
            steps {
                script {
                    def gitCommitHash = env.GIT_COMMIT
                    def imageTags = "${VERSION},${gitCommitHash}"
                    withDockerCredentials("dockerhub") {
                        sh "./gradlew jib \
                            -Djib.to.auth.username=${DOCKER_USERNAME} \
                            -Djib.to.auth.password=${DOCKER_PASSWORD} \
                            -Djib.to.tags=${imageTags} \
                            -PdockerRepositoryPrefix=h2oai/"
                    }
                }
            }
        }
    }
}

/**
 * Returns version specified in gradle.properties.
 *
 * Fails if master contains a release version (to prevent pushing release version accidentally).
 */
def getVersion() {
    def version = sh(
            script: "./gradlew -q -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false printVersion",
            returnStdout: true).trim()
    if (!version) {
        error "Version must be set"
    }
    if (isMasterBranch() && isReleaseVersion(version)) {
        error "Master contains a non-snapshot version"
    }
    return version
}

/**
 * Returns true, if the given version string denotes a release (not a snapshot) version.
 */
def isReleaseVersion(version) {
    return !version.endsWith("-SNAPSHOT")
}

/**
 * Returns true, if we are on the master branch.
 */
def isMasterBranch() {
    return env.BRANCH_NAME == "master"
}

/**
 * Returns true, if we are on a release branch.
 */
def isReleaseBranch() {
    return env.BRANCH_NAME.startsWith("release")
}

/** Context manager that runs content with set up credentials for a Docker repository. */
def withDockerCredentials(String credentialsId, Closure body) {
    def dockerCredentials = usernamePassword(
            credentialsId: credentialsId,
            passwordVariable: "DOCKER_PASSWORD",
            usernameVariable: "DOCKER_USERNAME"
    )
    withCredentials([dockerCredentials]) {
        body()
    }
}

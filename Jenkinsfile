@Library('shared-pipeline') import buildState.St
// A variable to facilitate dev env deployments from a feature branch
def milestoneCount    = 1
def st                = new St()
def app_name          = 'redis-event-logger'
def app_product       = 'map2'
def docker_image_name = 'nwea/redis-event-logger'
def nodeVersion       = '10'

def projectName       = 'redis-event-logger'
def snykOrganizationName = "platform-team"
def snykTokenId       = 'snyk-platform-team'
def severity          = 'high'

// BUILD FUNCTION
def npmInstall() {
    sh 'npm ci'
}
def npmConfigCacheValue() {
    return "${env.WORKSPACE}/npm_cache"
}
//Referenced https://stash.americas.nwea.pvt/projects/DEVOPS/repos/nwea-docker-example-app/browse

pipeline {
    agent none
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))          // Discard old Jenkins builds to avoid full disk space
    }
    environment {
       RC_ROOM_URL = 'https://hooks.glip.com/webhook/285efce3-d0f3-4057-ae5b-a3a928c8d16c'
    }
    tools {
        nodejs nodeVersion
    }
    stages {
        stage('inspection') {
            agent any
            environment {
                npm_config_cache  = npmConfigCacheValue()
            }
            steps {
                runSonarQubeAndSnykForSingleLambda(
                    "auth-event",
                    "${projectName}",
                    "${snykOrganizationName}",
                    "${snykTokenId}"
                )
            }
            post {
                cleanup {
                    cleanWs notFailBuild: true
                }
            }
        }
        stage('buildAndPublish') {
            agent {
                node {
                    label 'docker'
                }
            }
            tools {
                nodejs nodeVersion
            }
            steps {
                appInfo()
                script {
                    def data = readJSON text: '{ "buildNumber": "'+"${BUILD_ID}"+'" }'
                    writeJSON(file: 'jenkins_build.json', json: data, pretty: 4)
                }
                npmInstall()
                sh 'npm prune --production'
                version(st, sh(script: 'jq .version package.json', returnStdout: true).trim().replaceAll('', ''))   // Set the version number attached to the docker image
                publish(st, docker_image_name)                                                                     // Build and publish the docker image to DockerHub
            }
            post {
                always {
                cleanWs notFailBuild: true
                }
            }
        }
        // Team development/QA environment
        stage('deploy:team13:approval') {
            agent none
            when {
                branch 'develop'
            }
            steps {
                milestone milestoneCount++
                timeout(time: 1, unit: 'HOURS') {
                    input "Deploy this build to the Team13 environment?"
                }
                milestone milestoneCount++
            }
        }
        stage('deploy:team13') {
            when {
                branch 'develop'            // Restrict this stage to the develop branch
            }
            agent {
                node {
                    label 'docker'
                }
            }
            steps {
                script {
                    echo st.specifyEnv('team13')            // Set the environment to target
                }
                deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:256, timeout: 500])    // Deploy the application to the environment
            }
            post {
                always {
                    cleanWs notFailBuild: true          // Clean up workspace, so we don't fill up the build box
                }
                success {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment successful", '${st.env}')
                }
                failure {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment failed", '${st.env}')
                }
            }
        }
        stage('deploy:int03:approval') {
            agent none
            when {
                branch 'master'
            }
            steps {
                milestone milestoneCount++
                timeout(time: 1, unit: 'HOURS') {
                    input "Deploy this build to the INT03 environment?"
                }
                milestone milestoneCount++
            }
        }
        stage('deploy:int03') {
            when {
                branch 'master'            // Restrict this stage to the integration branch
            }
            agent {
                node {
                    label 'docker'
                }
            }
            steps {
                script {
                    echo st.specifyEnv('int03')
                }
                deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:512, timeout: 500])    // Deploy the application to the environment
            }
            post {
                always {
                    cleanWs notFailBuild: true          // Clean up workspace, so we don't fill up the build box
                }
                success {
                    deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:256, timeout: 500])    // Deploy the application to the environment
                }
            }
        }
        stage('deploy:stg01:approval') {
            agent none
            when {
                branch 'master'
            }
            steps {
                milestone milestoneCount++
                timeout(time: 1, unit: 'HOURS') {
                    input "Deploy this build to the STG01 environment?"
                }
                milestone milestoneCount++
            }
        }
        stage('deploy:stg01') {
            when {
                branch 'master'            // Restrict this stage to the staging branch
            }
            agent {
                node {
                    label 'docker'
                }
            }
            steps {
                script {
                    echo st.specifyEnv('stg01')
                }
                    deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:256, timeout: 500])    // Deploy the application to the environment
            }
            post {
                always {
                    cleanWs notFailBuild: true          // Clean up workspace, so we don't fill up the build box
                }
                success {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment successful", '${st.env}')
                }
            }
        }
         stage('deploy:perf01:approval') {
            agent none
            when {
                branch 'master'
            }
            steps {
                milestone milestoneCount++
                timeout(time: 1, unit: 'HOURS') {
                    input "Deploy this build to the PERF01 environment?"
                }
                milestone milestoneCount++
            }
        }
        stage('deploy:perf01') {
            when {
                branch 'master'          // Restrict this stage to the performance branch
            }
            agent {
                node {
                    label 'docker'
                }
            }
            steps {
                script {
                    echo st.specifyEnv('perf01')
                }
                deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:256, timeout: 500])    // Deploy the application to the environment
            }
            post {
                always {
                    cleanWs notFailBuild: true          // Clean up workspace, so we don't fill up the build box
                }
                success {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment successful", '${st.env}')
                }
            }
        }
         stage('deploy:prd:approval') {
            agent none
            when {
                branch 'master'
            }
            steps {
                milestone milestoneCount++
                timeout(time: 1, unit: 'HOURS') {
                    input "Deploy this build to the PRD environment?"
                }
                milestone milestoneCount++
            }
        }
        stage('publish') {
            agent any
            when {
                branch 'master'
            }
            steps {
                milestone milestoneCount++
                sh 'npm version minor -m "[ciskip] update package version to %s"'
                sshagent(credentials: ['9d289b9f-d686-45e2-a677-a37a79273464']) {
                    sh 'git push --follow-tags origin HEAD:master'
                }
                npmPublish()
            }
            post {
                always {
                    // Clean up workspace
                    cleanWs notFailBuild: true
                }
                failure {
                    notifyRingCentral('publish', 'failed')
                }
            }
        }
        stage('deploy:prd') {
            when {
                branch 'master'         // Restrict this stage to the master branch
            }
            agent {
                node {
                    label 'docker'
                }
            }
            steps {
                script {
                    echo st.specifyEnv('prd')
                }
                deploy(st, app_product, app_name, docker_image_name, "redis-event-logger.${st.env}.map2.nweacolo.pvt", [healthCheck: '/isAlive', memory:512, timeout: 500])    // Deploy the application to the environment
            }
            post {
                always {
                    cleanWs notFailBuild: true          // Clean up workspace, so we don't fill up the build box
                }
                success {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment successful", '${st.env}')
                }
                failure {
                    notifyRingCentral("redis-event-logger deploy:${st.env}", "redis-event-logger deployment failed", '${st.env}')
                }
            }
       }
    }
}
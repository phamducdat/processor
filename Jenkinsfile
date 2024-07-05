#!/usr/bin/env groovy

node {
    properties([disableConcurrentBuilds()])

    try {

        def remote = [:]
        remote.name = 'wiinvent-app-dev-01'
        remote.host = '10.130.88.63'
        remote.user = 'ipredict'
        remote.allowAnyHosts = true
        project = "gamicrm-stream-processor"
        dockerRepo = "dockerhub.infra.wiinvent.tv"
        imagePrefix = "gami"
        dockerFile = "Dockerfile"
        imageName = "${dockerRepo}/${imagePrefix}/${project}"
        buildNumber = "${env.BUILD_NUMBER}"
        IMAGE_BUILD = "${imageName}:${env.BRANCH_NAME}-build-${buildNumber}"
        k8sCluster = "local"
        k8sNameSpace = "gami-crm-prod"
        k8sEnv = "production"
        dockerComposeFile = "dev-docker-compose.yml"

        stage('checkout code') {
            checkout scm
            sh "git checkout ${env.BRANCH_NAME} && git reset --hard origin/${env.BRANCH_NAME}"
        }

        stage('build') {
            sh """
                egrep -q '^FROM .* AS builder\$' ${dockerFile} \
                && DOCKER_BUILDKIT=1 docker build -t ${imageName}-stage-builder --target builder -f ${dockerFile} .
                DOCKER_BUILDKIT=1 docker build -t ${imageName}:${env.BRANCH_NAME} -f ${dockerFile} .
            """
        }

        stage('push') {
            sh """
                docker push ${imageName}:${env.BRANCH_NAME}
                docker tag ${imageName}:${env.BRANCH_NAME} ${imageName}:${env.BRANCH_NAME}-build-${buildNumber}
                docker push ${imageName}:${env.BRANCH_NAME}-build-${buildNumber}
            """
        }

        switch (env.BRANCH_NAME) {
            case 'do-develop':
                stage('push docker-compose file') {
                    sh """
                        rsync -aurv ${dockerComposeFile} ${remote.host}::ipredict-apps/${project}/
                        export IMAGE_BUILD=${imageName}:${env.BRANCH_NAME}-build-${buildNumber}
                        ssh -o StrictHostKeyChecking=no ${remote.user}@${remote.host} 'export IMAGE_BUILD=${imageName}:${env.BRANCH_NAME}-build-${buildNumber} && cd /ipredict/${project}/ && docker-compose -f ${dockerComposeFile} down && docker-compose -f ${dockerComposeFile} up -d'
                    """
                }
                break
            case 'develop':
                k8sNameSpace = "gami-crm-dev"
                k8sEnv = "development"
                stage('deploy-prod') {
                    sh """
                    ## Deploy cluster LongVan
                    /usr/local/k8s/bin/k8sctl --cluster-name=${k8sCluster} --namespace=${k8sNameSpace} --environment=${k8sEnv} --service-name=${project} --image-name=${IMAGE_BUILD}
                  """
                }
                break
            case 'master':
                stage('deploy-prod') {
                    sh """
                    ## Deploy cluster LongVan
                    /usr/local/k8s/bin/k8sctl --cluster-name=${k8sCluster} --namespace=${k8sNameSpace} --environment=${k8sEnv} --service-name=${project} --image-name=${IMAGE_BUILD}
                  """
                }
                break

        }

    } catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
}

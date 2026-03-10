pipeline {
    agent none

    environment {
        PROJECT_DIR = 'facade'
        IMAGE_NAME = 'projetweb-backend'
        DOCKERHUB_CREDS = credentials('dockerhub-credentials')
        AGENT_IMAGE = "${DOCKERHUB_CREDS_USR}/project-jenkins-agent:latest"
        REGISTRY_IMAGE = "${DOCKERHUB_CREDS_USR}/${IMAGE_NAME}"
        STAGING_HOST = '34.155.133.83'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    stages {
        stage('Pull Agent Image') {
            agent any
            steps {
                sh '''
          echo "${DOCKERHUB_CREDS_PSW}" | docker login -u "${DOCKERHUB_CREDS_USR}" --password-stdin
          docker pull ${AGENT_IMAGE}
        '''
            }
        }

        stage('Prepare Workspace') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                // Force-delete any root-owned files from previous failed builds before checkout
                sh "rm -rf ${WORKSPACE}/* ${WORKSPACE}/.[!.]* ${WORKSPACE}/..?* || true"
                // Ensure workspace ownership is jenkins-friendly
                sh "chown -R 1000:1000 ${WORKSPACE} || true"
                sh "chmod -R u+rwX ${WORKSPACE} || true"
            }
        }

        stage('Checkout') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps { checkout scm }
        }

        stage('Build (Maven)') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            environment {
                TESTCONTAINERS_RYUK_DISABLED = 'true'
                MAVEN_USER_HOME = '/root'
            }
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'chmod +x mvnw'
                    sh './mvnw -B -ntp clean package -DskipTests'
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: "${PROJECT_DIR}/target/*.{jar,war}", fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                dir("${PROJECT_DIR}") {
                    sh '''
            # Build image using pre-built WAR in target/
            docker build \
              -t ${IMAGE_NAME}:${BUILD_NUMBER} \
              -t ${IMAGE_NAME}:latest \
              -f Dockerfile .
          '''
                }
            }
        }

        stage('Verify Docker Image') {
            agent any
            steps {
                sh '''
          echo "=== Verifying Docker Image ==="
          docker inspect ${IMAGE_NAME}:latest --format='Image ID: {{.Id}}'
          echo ""
          echo "=== Image Layers ==="
          docker history ${IMAGE_NAME}:latest --no-trunc | head -10
        '''
            }
        }

        stage('Push Image (Docker Hub)') {
            agent any
            steps {
                sh '''
      echo "${DOCKERHUB_CREDS_PSW}" | docker login -u "${DOCKERHUB_CREDS_USR}" --password-stdin

      docker tag ${IMAGE_NAME}:${BUILD_NUMBER} ${REGISTRY_IMAGE}:${BUILD_NUMBER}
      docker tag ${IMAGE_NAME}:latest        ${REGISTRY_IMAGE}:latest

      docker push ${REGISTRY_IMAGE}:${BUILD_NUMBER}
      docker push ${REGISTRY_IMAGE}:latest

      docker logout
    '''
            }
        }

        stage('Deploy to Staging') {
            agent any
            steps {
                withCredentials([sshUserPrivateKey(
      credentialsId: 'gcp-staging-ssh',
      keyFileVariable: 'SSH_KEY',
      usernameVariable: 'SSH_USER'
    )]) {
                    sh '''
        set -e
        echo "Deploying to ${STAGING_HOST} as ${SSH_USER}"

        # copy compose file
        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${SSH_USER}@${STAGING_HOST} "mkdir -p ~/deploy"
        scp -i "$SSH_KEY" -o StrictHostKeyChecking=no infra/deploy/staging-compose.yml ${SSH_USER}@${STAGING_HOST}:~/deploy/staging-compose.yml

        # run compose
        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${SSH_USER}@${STAGING_HOST} "
          set -e
          cd ~/deploy
                    echo ${DOCKERHUB_CREDS_PSW} | docker login -u ${DOCKERHUB_CREDS_USR} --password-stdin
          export APP_IMAGE=${DOCKERHUB_CREDS_USR}/${IMAGE_NAME}:${BUILD_NUMBER}
                    export APP_PORT=8082
                    docker compose -f staging-compose.yml pull
          docker compose -f staging-compose.yml up -d
                    docker logout
        "
      '''
    }
            }
        }
    }

    post {
        always {
            script {
                node {
                    sh 'docker images | grep ${IMAGE_NAME} || true'
                    sh "chown -R 1000:1000 ${WORKSPACE} || true"
                    deleteDir()
                }
            }
        }
    }
}

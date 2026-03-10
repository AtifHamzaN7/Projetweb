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
                    // Fix permissions on build artifacts so Jenkins (UID 1000) can delete them later
                    sh 'chmod -R 777 target/ 2>/dev/null || true'
                }
            }
            post {
                always {
                    archiveArtifacts(
                        allowEmptyArchive: true,
                        artifacts: "${PROJECT_DIR}/target/*.jar,${PROJECT_DIR}/target/*.war",
                        fingerprint: true
                    )
                }
            }
        }

        stage('Test (Backend)') {
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
                    sh './mvnw -B -ntp test'
                }
            }
            post {
                always {
                    junit testResults: "${PROJECT_DIR}/target/surefire-reports/TEST-*.xml", allowEmptyResults: true
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
        scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
          infra/deploy/staging-compose.yml \
          ${SSH_USER}@${STAGING_HOST}:~/deploy/staging-compose.yml

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

        stage('Post-Deploy Smoke Test') {
            agent any
            steps {
                sh """
                set -e

                BASE_URL="http://${STAGING_HOST}:8082"
                echo "Running staging smoke test on ${BASE_URL}"

                # Wait for API readiness after container restart.
                i=0
                STATUS=""
                while [ "\$i" -lt 30 ]; do
                    STATUS=\$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}/adherents" || true)
                    if [ "\$STATUS" = "200" ]; then
                        break
                    fi
                    i=\$((i + 1))
                    sleep 5
                done

                if [ "\$STATUS" != "200" ]; then
                    echo "Staging API is not ready after deploy (last status: \$STATUS)"
                    exit 1
                fi

                EMAIL="staging.smoke.${BUILD_NUMBER}.\$(date +%s)@example.com"
                PASSWORD="smoke123"

                CREATE_CODE=\$(curl -sS -o /tmp/smoke_create.out -w "%{http_code}" \
                    -X POST "${BASE_URL}/adherents/inscription" \
                    --data-urlencode "nom=Smoke" \
                    --data-urlencode "prenom=Test" \
                    --data-urlencode "email=\$EMAIL" \
                    --data-urlencode "password=\$PASSWORD")

                if [ "\$CREATE_CODE" != "200" ]; then
                    echo "Create adherent failed (HTTP \$CREATE_CODE)"
                    cat /tmp/smoke_create.out || true
                    exit 1
                fi

                LOGIN_BODY=\$(curl -sS "${BASE_URL}/adherents/connexion?email=\$EMAIL&password=\$PASSWORD")
                ID=\$(printf '%s' "\$LOGIN_BODY" | sed -n 's/.*"idAdh"[[:space:]]*:[[:space:]]*\\([0-9][0-9]*\\).*/\\1/p')

                if [ -z "\$ID" ]; then
                    echo "Login did not return idAdh"
                    echo "Response: \$LOGIN_BODY"
                    exit 1
                fi

                DELETE_CODE=\$(curl -sS -o /tmp/smoke_delete.out -w "%{http_code}" \
                    -X DELETE "${BASE_URL}/adherents/suppression/\$ID")
                if [ "\$DELETE_CODE" != "200" ]; then
                    echo "Delete adherent failed (HTTP \$DELETE_CODE)"
                    cat /tmp/smoke_delete.out || true
                    exit 1
                fi

                echo "Staging smoke test passed"
            """
            }
        }
    }

    post {
        always {
            script {
                node {
                    sh 'docker images | grep ${IMAGE_NAME} || true'
                    // Skip deleteDir() - let Jenkins handle workspace cleanup
                }
            }
        }
    }
}

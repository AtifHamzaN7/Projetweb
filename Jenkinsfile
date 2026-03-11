pipeline {
    agent none

    environment {
        PROJECT_DIR = 'facade'
        TEST_DIR = 'facade/src/test/java'
        IMAGE_NAME = 'projetweb-backend'
        COVERAGE_THRESHOLD = '50.00'

        DOCKERHUB_CREDS = credentials('dockerhub-credentials')
        AGENT_IMAGE = "${DOCKERHUB_CREDS_USR}/project-jenkins-agent:latest"
        REGISTRY_IMAGE = "${DOCKERHUB_CREDS_USR}/${IMAGE_NAME}"
        STAGING_HOST = '34.155.133.83'

        IMPACTED_TEST_FILTER = ''
        JACOCO_COVERAGE = ''
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
                sh "rm -rf ${WORKSPACE}/* ${WORKSPACE}/.[!.]* ${WORKSPACE}/..?* || true"
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
            steps {
                checkout scm
            }
        }

        stage('Ensure Full Git History') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                  git fetch --all --tags --prune || true
                  git fetch --unshallow || true
                '''
            }
        }

        stage('Check Docker (Testcontainers)') {
            agent any
            steps {
                sh 'docker info'
            }
        }

        stage('Generate Tests With AI') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'llm-api-key', variable: 'LLM_API_KEY')
                ]) {
                    sh '''
                      set -eu
                      (set -o pipefail) 2>/dev/null && set -o pipefail || true
                      NODE_MAJOR=0
                      if command -v node >/dev/null 2>&1; then
                        NODE_MAJOR="$(node -p "process.versions.node.split('.')[0]")"
                      fi
                      if [ "${NODE_MAJOR}" -lt 20 ]; then
                        echo "Node.js >=20 required. Installing Node 20..."
                        export DEBIAN_FRONTEND=noninteractive
                        apt-get update
                        apt-get install -y --no-install-recommends ca-certificates curl gnupg
                        install -d -m 0755 /etc/apt/keyrings
                        curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
                        echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" > /etc/apt/sources.list.d/nodesource.list
                        apt-get update
                        apt-get install -y --no-install-recommends nodejs
                      fi
                      git config --global --add safe.directory "${WORKSPACE}" || true
                      node --version
                      npm --version
                      LLM_API_KEY="$LLM_API_KEY" \
                      AI_TEST_REPAIR_ENABLED="1" \
                      AI_TEST_REPAIR_MAX_ITERS="2" \
                      node script-test.mjs
                    '''
                }
            }
        }

        stage('Load Impacted Test Filter') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                script {
                    env.IMPACTED_TEST_FILTER = sh(
                        script: '''
                          set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                          if [ -f "ai-test-filter.txt" ]; then
                            tr -d '\n' < ai-test-filter.txt | xargs
                          fi
                        ''',
                        returnStdout: true
                    ).trim()
                }

                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                  if [ -n "${IMPACTED_TEST_FILTER}" ]; then
                    echo "Impacted test filter: ${IMPACTED_TEST_FILTER}"
                    if [ -f "ai-impacted-tests.txt" ]; then
                      echo "Impacted test files:"
                      sed 's/^/- /' ai-impacted-tests.txt || true
                    fi
                    if [ -f "ai-impacted-classes.txt" ]; then
                      echo "Impacted production classes (for coverage):"
                      sed 's/^/- /' ai-impacted-classes.txt || true
                    fi
                  else
                    echo "Impacted test filter is empty. Tests will be skipped."
                  fi
                '''
            }
        }

        stage('Summarize Changed Files And Dependencies') {
            agent any
            steps {
                script {
                    if (fileExists('ai-deps-summary.txt')) {
                        echo '=== ai-deps-summary.txt ==='
                        echo readFile('ai-deps-summary.txt')
                    } else {
                        echo 'ai-deps-summary.txt missing (nothing to summarize)'
                    }
                }
            }
        }

        stage('Maven Verify (Skip Tests - No Impacted Tests)') {
            when {
                expression { return !env.IMPACTED_TEST_FILTER?.trim() }
            }
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'chmod +x mvnw'
                    sh './mvnw -B -ntp -DskipTests verify'
                    sh 'chmod -R 777 target/ 2>/dev/null || true'
                }
            }
        }

        stage('Maven Verify (Impacted Tests Only)') {
            when {
                expression { return env.IMPACTED_TEST_FILTER?.trim() }
            }
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
                    sh './mvnw -B -ntp -DfailIfNoTests=false -Dtest="${IMPACTED_TEST_FILTER}" verify'
                    sh 'chmod -R 777 target/ 2>/dev/null || true'
                }
            }
            post {
                always {
                    junit testResults: "${PROJECT_DIR}/target/surefire-reports/TEST-*.xml", allowEmptyResults: true
                }
            }
        }

        stage('Compute JaCoCo Coverage') {
            when {
                expression { return env.IMPACTED_TEST_FILTER?.trim() }
            }
            agent any
            steps {
                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true

                  CSV="facade/target/site/jacoco/jacoco.csv"
                  IMPACTED_CLASSES="ai-impacted-classes.txt"

                  if [ ! -f "$CSV" ]; then
                    echo "JaCoCo CSV not found at $CSV"
                    exit 1
                  fi

                  if [ -f "$IMPACTED_CLASSES" ] && [ -s "$IMPACTED_CLASSES" ]; then
                    TMP_PERCLASS="$(mktemp)"
                    TMP_UNMATCHED="$(mktemp)"
                    : > "$TMP_UNMATCHED"

                    awk -F, -v clsfile="$IMPACTED_CLASSES" -v perclass="$TMP_PERCLASS" -v unmatched="$TMP_UNMATCHED" '
                      BEGIN {
                        while ((getline line < clsfile) > 0) {
                          gsub(/\r/, "", line);
                          if (line != "") allow[line] = 1;
                        }
                        close(clsfile);
                      }
                      NR==1 { next }
                      {
                        key = $2 "." $3;
                        if (allow[key]) {
                          missed += $4;
                          covered += $5;
                          classMiss[key] += $4;
                          classCov[key] += $5;
                          matched[key] = 1;
                        }
                      }
                      END {
                        matchedCount = 0;
                        for (k in matched) matchedCount += 1;

                        for (k in allow) {
                          if (!(k in matched)) print k > unmatched;
                        }
                        close(unmatched);

                        for (k in matched) {
                          tm = classMiss[k] + classCov[k];
                          pct = (tm == 0) ? 0 : (classCov[k] / tm) * 100;
                          printf "%.2f\t%s\t%d\t%d\\n", pct, k, classMiss[k], classCov[k] > perclass;
                        }
                        close(perclass);

                        total = missed + covered;
                        if (matchedCount == 0 || total == 0) {
                          printf "0.00";
                        } else {
                          printf "%.2f", (covered/total)*100;
                        }
                      }
                    ' "$CSV" > coverage.txt

                    rm -f "$TMP_PERCLASS" "$TMP_UNMATCHED" || true
                  else
                    awk -F, '
                      NR>1 { missed += $4; covered += $5; }
                      END {
                        total = missed + covered;
                        if (total == 0) { printf "0.00"; }
                        else { printf "%.2f", (covered/total)*100; }
                      }
                    ' "$CSV" > coverage.txt
                  fi

                  COV=$(cat coverage.txt)
                  echo "$COV" > .jacoco-coverage
                  echo "JaCoCo instruction coverage: $COV%"
                '''

                script {
                    env.JACOCO_COVERAGE = readFile('.jacoco-coverage').trim()
                }
            }
        }

        stage('Enforce Coverage Threshold (50%)') {
            when {
                expression { return env.IMPACTED_TEST_FILTER?.trim() }
            }
            agent any
            steps {
                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                  THRESHOLD="${COVERAGE_THRESHOLD}"
                  COV="${JACOCO_COVERAGE}"

                  PASS=$(awk -v c="$COV" -v t="$THRESHOLD" 'BEGIN { print (c+0 >= t+0) ? "yes" : "no" }')

                  if [ "$PASS" = "yes" ]; then
                    echo "Coverage OK: $COV% (threshold $THRESHOLD%)"
                    exit 0
                  fi

                  echo "Coverage too low: $COV% (threshold $THRESHOLD%)"
                  exit 1
                '''
            }
        }

        stage('Commit Generated Tests To Branch') {
            when {
                allOf {
                    expression { return !env.CHANGE_ID?.trim() }
                    expression { return env.BRANCH_NAME?.trim() }
                }
            }
            agent any
            steps {
                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true

                  git config user.name "github-actions[bot]"
                  git config user.email "github-actions[bot]@users.noreply.github.com"

                  BRANCH="${BRANCH_NAME}"
                  git checkout "$BRANCH"
                  git add facade/src/test/java || true

                  if git diff --cached --quiet; then
                    echo "No generated tests to commit."
                    exit 0
                  fi

                  git commit -m "test(ai): add generated tests [skip ci]"
                  git push origin "HEAD:$BRANCH"
                '''
            }
        }

        stage('Archive Generated Tests') {
            agent any
            steps {
                archiveArtifacts(
                    allowEmptyArchive: true,
                    artifacts: "${TEST_DIR}/**/*.java"
                )
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
                  docker tag ${IMAGE_NAME}:latest ${REGISTRY_IMAGE}:latest

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

                      ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${SSH_USER}@${STAGING_HOST} "mkdir -p ~/deploy"
                      scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
                        infra/deploy/staging-compose.yml \
                        ${SSH_USER}@${STAGING_HOST}:~/deploy/staging-compose.yml

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
                sh '''
                  set -e

                  BASE_URL="http://${STAGING_HOST}:8082"
                  echo "Running staging smoke test on ${BASE_URL}"

                  i=0
                  STATUS=""
                  while [ "$i" -lt 30 ]; do
                    STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "${BASE_URL}/adherents" || true)
                    if [ "$STATUS" = "200" ]; then
                      break
                    fi
                    i=$((i + 1))
                    sleep 5
                  done

                  if [ "$STATUS" != "200" ]; then
                    echo "Staging API is not ready after deploy (last status: $STATUS)"
                    exit 1
                  fi

                  EMAIL="staging.smoke.${BUILD_NUMBER}.$(date +%s)@example.com"
                  PASSWORD="smoke123"

                  CREATE_CODE=$(curl -sS -o /tmp/smoke_create.out -w "%{http_code}" -X POST "${BASE_URL}/adherents/inscription" --data-urlencode "nom=Smoke" --data-urlencode "prenom=Test" --data-urlencode "email=$EMAIL" --data-urlencode "password=$PASSWORD")
                  if [ "$CREATE_CODE" != "200" ]; then
                    echo "Create adherent failed (HTTP $CREATE_CODE)"
                    cat /tmp/smoke_create.out || true
                    exit 1
                  fi

                  LOGIN_BODY=$(curl -sS "${BASE_URL}/adherents/connexion?email=$EMAIL&password=$PASSWORD")
                  ID=$(printf '%s' "$LOGIN_BODY" | grep -oE '"idAdh"[[:space:]]*:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -n 1)
                  if [ -z "$ID" ]; then
                    echo "Login did not return idAdh"
                    echo "Response: $LOGIN_BODY"
                    exit 1
                  fi

                  DELETE_CODE=$(curl -sS -o /tmp/smoke_delete.out -w "%{http_code}" -X DELETE "${BASE_URL}/adherents/suppression/$ID")
                  if [ "$DELETE_CODE" != "200" ]; then
                    echo "Delete adherent failed (HTTP $DELETE_CODE)"
                    cat /tmp/smoke_delete.out || true
                    exit 1
                  fi

                  echo "Staging smoke test passed"
                '''
            }
        }
    }

    post {
        always {
            script {
                node {
                    sh 'docker images | grep ${IMAGE_NAME} || true'
                }
            }
        }
        failure {
            script {
                if (env.CHANGE_ID?.trim()) {
                    withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
                        sh '''
                          set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                          if ! command -v gh >/dev/null 2>&1; then
                            echo "gh CLI not found. Skipping PR failure comment."
                            exit 0
                          fi

                          COV="${JACOCO_COVERAGE:-N/A}"
                          THRESHOLD="${COVERAGE_THRESHOLD}%"
                          gh pr comment "${CHANGE_ID}" --body "CI blocked: insufficient JaCoCo coverage

                          Current instruction coverage: ${COV}%
                          Required threshold: ${THRESHOLD}

                          Some tests may have been generated automatically and archived as build artifacts.
                          Please improve tests and rerun CI." || true
                        '''
                    }
                }
            }
        }
    }
}

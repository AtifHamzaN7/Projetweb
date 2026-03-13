pipeline {
    agent none

    environment {
        PROJECT_DIR = 'facade'
        TEST_DIR = 'facade/src/test/java'
        IMAGE_NAME = 'projetweb-backend'
        EVIDENCE_DIR = 'evidence'
        COVERAGE_THRESHOLD = '50.00'

        DOCKERHUB_CREDS = credentials('dockerhub-credentials')
        AGENT_IMAGE = "${DOCKERHUB_CREDS_USR}/project-jenkins-agent:latest"
        REGISTRY_IMAGE = "${DOCKERHUB_CREDS_USR}/${IMAGE_NAME}"
        STAGING_HOST = '34.155.133.83'
        STAGING_SERVICE_NAME = 'app'
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
                  git config --global --add safe.directory "${WORKSPACE}" || true
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
                      AI_TEST_REPAIR_STRICT="0" \
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
                            tr -d '\r\n' < ai-test-filter.txt
                          fi
                        ''',
                        returnStdout: true
                    ).trim()
                }

                sh '''
                  set -eu
                  (set -o pipefail) 2>/dev/null && set -o pipefail || true
                  if [ -n "${IMPACTED_TEST_FILTER:-}" ]; then
                    echo "Impacted test filter: ${IMPACTED_TEST_FILTER:-}"
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
                withCredentials([
                    usernamePassword(
                        credentialsId: 'github-pat',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_PASSWORD'
                    )
                ]) {
                    sh '''
                      set -eu
                      (set -o pipefail) 2>/dev/null && set -o pipefail || true

                      git config --global --add safe.directory "${WORKSPACE}" || true
                      git config user.name "github-actions[bot]"
                      git config user.email "github-actions[bot]@users.noreply.github.com"

                      BRANCH="${BRANCH_NAME}"
                      REMOTE_URL="https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/AtifHamzaN7/Projetweb.git"

                      git remote set-url origin "$REMOTE_URL"
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
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'gcp-staging-ssh',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    ),
                    string(credentialsId: 'llm-api-key', variable: 'LLM_API_KEY')
                ]) {
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
                script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        sh '''
                          set -eu
                          mkdir -p "${EVIDENCE_DIR}"
                          SMOKE_FILE="${EVIDENCE_DIR}/smoke_test.txt"
                          HEALTH_FILE="${EVIDENCE_DIR}/health.txt"
                          : > "$SMOKE_FILE"

                          log() {
                            printf '[%s] %s\n' "$(date -u +%FT%TZ)" "$*" | tee -a "$SMOKE_FILE"
                          }

                          BASE_URL="http://${STAGING_HOST}:8082"
                          log "Running staging smoke test on ${BASE_URL}"

                          FAIL=0
                          i=0
                          STATUS=""
                          while [ "$i" -lt 30 ]; do
                            STATUS=$(curl -sS -o /tmp/smoke_probe.out -w "%{http_code}" "${BASE_URL}/adherents" || true)
                            log "Probe /adherents HTTP ${STATUS}"
                            if [ "$STATUS" = "200" ]; then
                              break
                            fi
                            i=$((i + 1))
                            sleep 5
                          done

                          if [ "$STATUS" != "200" ]; then
                            log "Staging API is not ready after deploy (last status: $STATUS)"
                            FAIL=1
                          fi

                          if [ "$FAIL" -eq 0 ]; then
                            EMAIL="staging.smoke.${BUILD_NUMBER}.$(date +%s)@example.com"
                            PASSWORD="smoke123"

                            CREATE_CODE=$(curl -sS -o /tmp/smoke_create.out -w "%{http_code}" -X POST "${BASE_URL}/adherents/inscription" --data-urlencode "nom=Smoke" --data-urlencode "prenom=Test" --data-urlencode "email=$EMAIL" --data-urlencode "password=$PASSWORD")
                            log "POST /adherents/inscription HTTP ${CREATE_CODE}"
                            if [ "$CREATE_CODE" != "200" ]; then
                              log "Create adherent failed (HTTP ${CREATE_CODE})"
                              head -c 300 /tmp/smoke_create.out | tr '\n' ' ' | sed 's/^/[body] /' | tee -a "$SMOKE_FILE" >/dev/null || true
                              FAIL=1
                            fi

                            LOGIN_BODY=$(curl -sS "${BASE_URL}/adherents/connexion?email=$EMAIL&password=$PASSWORD" || true)
                            ID=$(printf '%s' "$LOGIN_BODY" | grep -oE '"idAdh"[[:space:]]*:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -n 1 || true)
                            if [ -z "$ID" ]; then
                              log "Login did not return idAdh"
                              printf '[%s] [body] %s\n' "$(date -u +%FT%TZ)" "$(printf '%s' "$LOGIN_BODY" | tr '\n' ' ' | head -c 300)" | tee -a "$SMOKE_FILE" >/dev/null
                              FAIL=1
                            else
                              DELETE_CODE=$(curl -sS -o /tmp/smoke_delete.out -w "%{http_code}" -X DELETE "${BASE_URL}/adherents/suppression/$ID")
                              log "DELETE /adherents/suppression/${ID} HTTP ${DELETE_CODE}"
                              if [ "$DELETE_CODE" != "200" ]; then
                                log "Delete adherent failed (HTTP ${DELETE_CODE})"
                                head -c 300 /tmp/smoke_delete.out | tr '\n' ' ' | sed 's/^/[body] /' | tee -a "$SMOKE_FILE" >/dev/null || true
                                FAIL=1
                              fi
                            fi
                          fi

                          HEALTH_CODE=$(curl -sS -o /tmp/health_body.out -w "%{http_code}" "${BASE_URL}/actuator/health" || true)
                          {
                            printf '[%s] GET /actuator/health HTTP %s\n' "$(date -u +%FT%TZ)" "${HEALTH_CODE}"
                            printf '[%s] body ' "$(date -u +%FT%TZ)"
                            head -c 300 /tmp/health_body.out | tr '\n' ' ' || true
                            printf '\n'
                          } > "$HEALTH_FILE"

                          if [ "$FAIL" -eq 0 ]; then
                            log "Staging smoke test passed"
                          else
                            log "Staging smoke test failed"
                            exit 1
                          fi
                        '''
                    }
                }
            }
        }

        stage('Deployment Verifier & Triage (Staging)') {
            agent {
                docker {
                    image "${AGENT_IMAGE}"
                    args '-u root:root -v /var/run/docker.sock:/var/run/docker.sock -v /var/jenkins_home/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'gcp-staging-ssh',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    ),
                    string(credentialsId: 'llm-api-key', variable: 'LLM_API_KEY')
                ]) {
                    sh '''
                      set +e
                      mkdir -p "${EVIDENCE_DIR}"

                      DEPLOY_TS="$(date -u +%FT%TZ)"
                      COMMIT_SHA="${GIT_COMMIT:-}"
                      if [ -z "${COMMIT_SHA}" ]; then
                        COMMIT_SHA="$(git rev-parse HEAD 2>/dev/null || true)"
                      fi
                      if [ -z "${COMMIT_SHA}" ]; then
                        COMMIT_SHA="unknown"
                      fi
                      printf '{\n  "build_number": "%s",\n  "image": "%s",\n  "commit_sha": "%s",\n  "deploy_timestamp_utc": "%s",\n  "staging_host": "%s"\n}\n' \
                        "${BUILD_NUMBER}" \
                        "${REGISTRY_IMAGE}:${BUILD_NUMBER}" \
                        "${COMMIT_SHA}" \
                        "${DEPLOY_TS}" \
                        "${STAGING_HOST}" \
                        > "${EVIDENCE_DIR}/deploy_meta.json"

                      ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${SSH_USER}@${STAGING_HOST} \
                        "cd ~/deploy && docker compose -f staging-compose.yml ps" \
                        > "${EVIDENCE_DIR}/compose_ps.txt" 2>&1 || true

                      ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${SSH_USER}@${STAGING_HOST} \
                        "cd ~/deploy && docker compose -f staging-compose.yml logs --tail 500 ${STAGING_SERVICE_NAME}" \
                        > "${EVIDENCE_DIR}/container_logs.txt" 2>&1 || true

                      PYTHON_BIN="$(command -v python3 || command -v python || true)"
                      if [ -n "$PYTHON_BIN" ]; then
                        "$PYTHON_BIN" agents/agent1/analyze-deploy.py \
                          --evidence_dir "${EVIDENCE_DIR}" \
                          --out "${EVIDENCE_DIR}/agent_report.json" \
                          --md_out "${EVIDENCE_DIR}/agent_report.md" \
                          --service_name "${STAGING_SERVICE_NAME}" \
                          --build_number "${BUILD_NUMBER}" \
                          --image "${REGISTRY_IMAGE}:${BUILD_NUMBER}" \
                          --staging_host "${STAGING_HOST}" || true
                      else
                        echo '{"error":"python interpreter not available for deployment analyzer"}' > "${EVIDENCE_DIR}/agent_report.json"
                      fi

                      exit 0
                    '''
                }
            }
        }

    }

    post {
        always {
            node('') {
                script {
                    sh 'docker images | grep ${IMAGE_NAME} || true'
                    archiveArtifacts(
                        allowEmptyArchive: true,
                        artifacts: "${EVIDENCE_DIR}/**"
                    )
                }
            }
        }
        failure {
            node('') {
                script {
                    sh '''
                      set +e
                      mkdir -p "${EVIDENCE_DIR}"
                      if [ ! -f "${EVIDENCE_DIR}/agent_report.json" ] && [ -f "agents/agent1/analyze-deploy.py" ]; then
                        PYTHON_BIN="$(command -v python3 || command -v python || true)"
                        if [ -n "$PYTHON_BIN" ]; then
                          "$PYTHON_BIN" agents/agent1/analyze-deploy.py \
                            --evidence_dir "${EVIDENCE_DIR}" \
                            --out "${EVIDENCE_DIR}/agent_report.json" \
                            --md_out "${EVIDENCE_DIR}/agent_report.md" \
                            --service_name "${STAGING_SERVICE_NAME}" \
                            --build_number "${BUILD_NUMBER}" \
                            --image "${REGISTRY_IMAGE}:${BUILD_NUMBER}" \
                            --staging_host "${STAGING_HOST}" || true
                        fi
                    fi
                      exit 0
                    '''
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
}

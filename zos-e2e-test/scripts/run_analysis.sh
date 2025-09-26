#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "SONAR_TOKEN"
check_env_variable "JOB_TIMESTAMP"
check_env_variable "ZOS_USER_HOST"

ZOS_PRIVATE_KEY=$GITHUB_WORKSPACE/zos_key
REMOTE_DEST_DIR="tmp/$JOB_TIMESTAMP"
PROJECT_KEY="zos-e2e-test-$JOB_TIMESTAMP"
ssh -i "$ZOS_PRIVATE_KEY" "$ZOS_USER_HOST" << EOF
  chmod +x $REMOTE_DEST_DIR/sonar-scanner-*/bin/sonar-scanner
  cd $REMOTE_DEST_DIR/sample_project
  bash ../sonar-scanner-*/bin/sonar-scanner \
    -Dsonar.projectKey=$PROJECT_KEY \
    -Dsonar.sources=. \
    -Dsonar.userHome=/SYSTEM/tmp \
    -Dsonar.host.url=https://zena.sonarsource.com \
    -Dsonar.token=$SONAR_TOKEN \
    -Dsonar.scanner.skipJreProvisioning=true \
    -Dsonar.scm.disabled=true \
    -Dsonar.lang.patterns.cobol=**/*
EOF

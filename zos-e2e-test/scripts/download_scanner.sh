#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "ZOS_USER_HOST"
check_env_variable "JOB_TIMESTAMP"
check_env_variable "GITHUB_TOKEN"

ZOS_PRIVATE_KEY=$GITHUB_WORKSPACE/zos_key

SONAR_SCANNER_VERSION=$(curl -sSL -H "Accept: application/vnd.github+json" \
  -H "authorization: Bearer $GITHUB_TOKEN" \
  https://api.github.com/repos/SonarSource/sonar-scanner-cli/releases/latest | jq -r '.tag_name')
check_status "Failed to fetch latest sonar-scanner version from GitHub API"

echo "sonar-scanner-version=${SONAR_SCANNER_VERSION}"

REMOTE_DEST_DIR="tmp/$JOB_TIMESTAMP"
SONAR_SCANNER_ARCHIVE="sonar-scanner-cli-${SONAR_SCANNER_VERSION}.zip"
SONAR_SCANNER_URL="https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/${SONAR_SCANNER_ARCHIVE}"
ssh -i "$ZOS_PRIVATE_KEY" "$ZOS_USER_HOST" << EOF
  mkdir -p $REMOTE_DEST_DIR
  cd $REMOTE_DEST_DIR
  curl -sSL "${SONAR_SCANNER_URL}" -o "$SONAR_SCANNER_ARCHIVE"
  jar -xvf $SONAR_SCANNER_ARCHIVE
EOF

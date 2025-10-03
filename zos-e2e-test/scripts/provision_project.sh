#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "ZOS_USER_HOST"
check_env_variable "JOB_TIMESTAMP"

PROJECT_ROOT_DIR="$1"
if [ -z "$PROJECT_ROOT_DIR" ]; then
  echo "Project root directory not provided. Exiting."
  exit 1
fi

ZOS_PRIVATE_KEY=$GITHUB_WORKSPACE/zos_key
REMOTE_PROJECT_DEST="tmp/$JOB_TIMESTAMP"

echo "Creating directory $REMOTE_PROJECT_DEST..."

ssh -i "$ZOS_PRIVATE_KEY" $ZOS_USER_HOST "mkdir -p $REMOTE_PROJECT_DEST"

echo "Uploading project directory $PROJECT_ROOT_DIR to remote system..."
if ! scp -i "$ZOS_PRIVATE_KEY" -rO "$PROJECT_ROOT_DIR" "$ZOS_USER_HOST":"$REMOTE_PROJECT_DEST"
then
  echo "Failed to upload project directory $PROJECT_ROOT_DIR. Exiting."
  exit 1
fi




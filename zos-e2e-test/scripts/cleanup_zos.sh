#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "ZOS_USER_HOST"

ZOS_PRIVATE_KEY=$GITHUB_WORKSPACE/zos_key

echo "Cleaning up files on z/OS system:"
ssh -i "$ZOS_PRIVATE_KEY" "$ZOS_USER_HOST" "rm -rfvP tmp"

#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "SONAR_TOKEN"
check_env_variable "JOB_TIMESTAMP"

PROJECT_KEY="zos-e2e-test-$JOB_TIMESTAMP"
echo "Deleting project $PROJECT_KEY from Zena..."

response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  "https://zena.sonarsource.com/api/projects/delete" \
  -H "Authorization: Bearer $SONAR_TOKEN" \
  -d "project=$PROJECT_KEY")

if [[ "$response" != "204" ]]; then
  echo "Failed to delete project. HTTP status: $response"
  exit 1
fi

echo "Project $PROJECT_KEY deleted successfully."

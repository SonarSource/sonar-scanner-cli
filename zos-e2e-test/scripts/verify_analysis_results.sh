#!/usr/bin/env bash

source "$(dirname -- "$0")/utils.sh"

check_env_variable "SONAR_TOKEN"
check_env_variable "JOB_TIMESTAMP"

PROJECT_KEY="zos-e2e-test-$JOB_TIMESTAMP"


#We loop the search for project because it may take some time for the analysis to be processed
start_time=$(date +%s)
timeout=180

while true; do
  response=$(curl -s -H "Authorization: Bearer $SONAR_TOKEN" \
    "https://zena.sonarsource.com/api/issues/search?components=$PROJECT_KEY")

  components_count=$(echo "$response" | jq '.components | length')
  if [[ "$components_count" -gt 0 ]]; then
    break
  fi

  current_time=$(date +%s)
  elapsed=$((current_time - start_time))
  if [[ "$elapsed" -ge "$timeout" ]]; then
    echo "Timeout: No project found within $timeout seconds."
    exit 1
  fi

  sleep 5
done

total=$(echo "$response" | jq '.paging.total')

if [[ "$total" -gt 1 ]]; then
  echo "Success: $total issue(s) found."
else
  echo "Failure: Not enough issues found (total=$total)."
  exit 1
fi

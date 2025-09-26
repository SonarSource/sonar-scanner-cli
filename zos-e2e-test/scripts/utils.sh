#!/usr/bin/env bash

check_status() {
  exit_status=$?
  if [ $exit_status -ne 0 ]; then
    echo "::error::$1"
    exit $exit_status
  fi
}

check_env_variable() {
  var_name=$1
  if [ -z "${!var_name}" ]; then
    echo "::error::Environment variable '$var_name' is not set."
    exit 1
  fi
}

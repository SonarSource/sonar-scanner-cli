#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v10/install.sh | bash
}

installTravisTools
travis_build_green "SonarSource/sonarqube" "master"
mvn verify -B -e -V

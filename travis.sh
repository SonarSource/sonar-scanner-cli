#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v2.1/install.sh | bash
}

installTravisTools
travis_build_green_sonarqube_snapshot
mvn verify -B -e -V

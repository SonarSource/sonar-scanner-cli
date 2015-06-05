#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/dgageot/travis-utils/master/install.sh | sh
  source /tmp/travis-utils/utils.sh
}

installTravisTools
build_green_sonarqube_snapshot
mvn verify -B -e -V

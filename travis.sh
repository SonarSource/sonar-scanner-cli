#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v12/install.sh | bash
  source /tmp/travis-utils/env.sh
}

case "$TESTS" in

CI)
  installTravisTools

  travis_build_green "SonarSource/sonarqube" "master"
  mvn verify -B -e -V
  ;;

IT-DEV)
  installTravisTools

  travis_build_green "SonarSource/sonarqube" "master"
  mvn install -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd it
  mvn -DsonarRunner.version="2.5-SNAPSHOT" -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac

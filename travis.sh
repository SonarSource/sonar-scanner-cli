#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v13/install.sh | bash
  source /tmp/travis-utils/env.sh
}

function buildSnapshotDependencies {
  travis_build "SonarSource/sonar-orchestrator" "0fe60edd0978300334ecc9101e4c10bcb05516d0"
  travis_build_green "SonarSource/sonarqube" "master"
}

case "$TESTS" in

CI)
  installTravisTools
  buildSnapshotDependencies

  mvn verify -B -e -V
  ;;

IT-DEV)
  installTravisTools
  buildSnapshotDependencies

  mvn install -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd it
  mvn -DsonarRunner.version="2.5-SNAPSHOT" -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac

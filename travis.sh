#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v11/install.sh | bash
  source /tmp/travis-utils/env.sh
}

case "$TESTS" in

CI)
  installTravisTools
  travis_build_green "SonarSource/sonarqube" "master"
  mvn verify -B -e -V
  ;;

IT-DEV)
  cat /etc/hosts
  rpm -qa | grep glibc
  installTravisTools
  mvn install -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true
  travis_build_green "SonarSource/sonarqube" "master"
  cd it
  mvn -Dsonar.runtimeVersion="DEV" -DsonarRunner.version="2.5-SNAPSHOT" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac

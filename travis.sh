#!/bin/bash -v

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v27 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis


case "$TARGET" in

CI)
  regular_mvn_build_deploy_analyze
  ;;

IT)
  MIN_SQ_VERSION="LTS"
  mvn install -DskipTests=true -Dsource.skip=true -Denforcer.skip=true -B -e -V

  echo '======= Run integration tests on minimal supported version of SonarQube ($MIN_SQ_VERSION)'
  cd it
  mvn -Dsonar.runtimeVersion="$MIN_SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false verify -e -B -V
  # all other versions of SQ are tested by the QA pipeline at SonarSource
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac


#!/bin/bash -v

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v27 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

regular_mvn_build_deploy_analyze


MIN_SQ_VERSION="LTS"
echo '======= Run integration tests on minimal supported version of SonarQube ($MIN_SQ_VERSION)'
cd it
mvn -Dsonar.runtimeVersion="$MIN_SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false verify -e -B -V


# all other versions of SQ are tested by the QA pipeline at SonarSource

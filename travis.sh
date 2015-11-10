#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

if [ -n "${PR_ANALYSIS:-}" ] && [ "${PR_ANALYSIS}" == true ]
then
  if [ "$TRAVIS_PULL_REQUEST" != "false" ]
  then
    # For security reasons environment variables are not available on the pull requests
    # coming from outside repositories
    # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
    if [ -n "$SONAR_GITHUB_OAUTH" ]; then

      # Switch to java 8 as the Dory HTTPS certificate is not supported by Java 7
      export JAVA_HOME=/usr/lib/jvm/java-8-oracle
      export PATH=$JAVA_HOME/bin:$PATH

      # PR analysis
      mvn verify sonar:sonar -B -e -V \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
        -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_LOGIN \
        -Dsonar.password=$SONAR_PASSWD  
    fi
  fi
else
  # Regular CI (use install for ITs)
  mvn install -B -e -V
fi

if [ -n "${RUN_ITS:-}" ] && [ "${RUN_ITS}" == true ]
then

  installTravisTools
  build_snapshot "SonarSource/orchestrator"

  if [ "${SQ_VERSION}" == "DEV" ]
  then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd it
  mvn -DsonarRunner.version="2.5-SNAPSHOT" -Dsonar.runtimeVersion=$SQ_VERSION -Dmaven.test.redirectTestOutputToFile=false install

fi

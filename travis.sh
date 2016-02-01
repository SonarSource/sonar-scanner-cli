#!/bin/bash -v

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v23 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

case "$TARGET" in

CI)
  if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho 'Build, deploy and analyze master'

    SONAR_PROJECT_VERSION=`maven_expression "project.version"`

    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER

    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
        -Pcoverage-per-test,deploy-sonarsource \
        -Dmaven.test.redirectTestOutputToFile=false \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -Dsonar.projectVersion=$SONAR_PROJECT_VERSION \
        -B -e -V

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
    strongEcho 'Build and analyze pull request, no deploy'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.

    mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
        -Dmaven.test.redirectTestOutputToFile=false \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN \
        -B -e -V

  else
    strongEcho 'Build, no analysis, no deploy'

    # No need for Maven phase "install" as the generated JAR file does not need to be installed
    # in Maven local repository. Phase "verify" is enough.

    mvn verify \
        -Dmaven.test.redirectTestOutputToFile=false \
        -B -e -V
  fi
  ;;

IT)
  if [ "${SQ_VERSION}" = "DEV" ]
  then
    build_snapshot "SonarSource/sonarqube"
  fi

  # Need install because ITs will take artifact from local repo
  mvn install -B -e -V -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true
  
  cd it
  
  mvn -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false verify -e -B -V
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac

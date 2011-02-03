#!/bin/sh
#
# Sonar Runner Startup Script for Unix
#

if [ -z "$SONAR_RUNNER_HOME" ] ; then
  PRG="$0"

  SONAR_RUNNER_HOME=`dirname "$PRG"`/..

  # make it fully qualified
  SONAR_RUNNER_HOME=`cd "$SONAR_RUNNER_HOME" && pwd`
fi

echo "Info: Using sonar-runner at $SONAR_RUNNER_HOME"

JAVACMD="`which java`"

exec "$JAVACMD"
  -classpath "${SONAR_RUNNER_HOME}"/lib/sonar-runner-*.jar \
  "$@"

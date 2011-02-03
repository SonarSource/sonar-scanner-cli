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

JAVACMD="`which java`"

JAVACLASSPATH="${SONAR_RUNNER_HOME}"/lib/sonar-runner.jar
JAVACLASSPATH=$JAVACLASSPATH:"${SONAR_RUNNER_HOME}"/lib/sonar-batch-bootstrapper.jar

#echo "Info: Using sonar-runner at $SONAR_RUNNER_HOME"
#echo "Info: Using java at $JAVACMD"
#echo "Info: Using classpath $JAVACLASSPATH"

exec "$JAVACMD" \
  -classpath $JAVACLASSPATH \
  "-Drunner.home=${SONAR_RUNNER_HOME}" \
  org.sonar.runner.Main "$@"

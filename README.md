SonarQube Runner [![Build Status](https://travis-ci.org/SonarSource/sonar-runner.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-runner)
=========================

Bootstrapper of code analysis

Documentation:
http://docs.sonarqube.org/display/SONAR/Analyzing+with+SonarQube+Runner

Issue Tracker:
http://jira.sonarsource.com/browse/SONARUNNER

Release:
sonar-runner-api need to be signed for use in SonarLint for Eclipse. So you need to pass following properties during perform:
mvn release:perform -Djarsigner.keystore=<path to keystore.jks> -Djarsigner.storepass=<password>
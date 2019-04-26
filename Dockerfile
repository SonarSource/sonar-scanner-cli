FROM openjdk:8-jre-alpine

LABEL maintainer="nanaflat <nanoflat@gmail.com>"

ARG SONAR_SCANNER_VERSION

RUN apk add --no-cache curl sed unzip nodejs yarn

# Settings
ENV SONAR_URL="https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux.zip"
ENV SONAR_RUNNER_HOME="/sonar-scanner-${SONAR_SCANNER_VERSION}-linux"
ENV PATH $PATH:$SONAR_RUNNER_HOME/bin

# Install sonar-scanner
RUN curl -o ./sonarscanner.zip -L $SONAR_URL
RUN unzip sonarscanner.zip 
RUN rm sonarscanner.zip

# Ensure Sonar Scanner uses openjdk instead of the packaged JRE (which is broken)
RUN sed -i 's/use_embedded_jre=true/use_embedded_jre=false/g' $SONAR_RUNNER_HOME/bin/sonar-scanner
#------------------------------------------------------------------------------
# Installs NodeJS, which is needed for running the Linux ITs.
#
# Build from the basedir:
#   docker build -f it/docker/Dockerfile-qa -t sonar-scanner-cli-qa it/docker
#
# Verify the content of the image by running a shell session in it:
#   docker run -it sonar-scanner-cli-qa bash
#
# CirrusCI builds the image when needed. No need to manually upload it to
# Google Cloud Container Registry. See section "gke_container" of .cirrus.yml
#------------------------------------------------------------------------------

FROM gcr.io/sonarqube-team/base:latest

USER root

RUN curl -sL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get install -y nodejs

USER sonarsource

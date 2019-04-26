#!/bin/bash
#assume latest version from tag
SONAR_SCANNER_VERSION=$(git describe --tags |cut -d- -f1)
echo Scanner version is ${SONAR_SCANNER_VERSION}

#build docker image
docker build \
      --pull \
      --build-arg SONAR_SCANNER_VERSION="${SONAR_SCANNER_VERSION}" \
      -t "${DOCKERHUB_USERNAME}/sonar-scanner:${SONAR_SCANNER_VERSION}" \
      -t "${DOCKERHUB_USERNAME}/sonar-scanner:latest" \
      .

#push docker image to dockerhub
echo "$DOCKERHUB_PASSWORD" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin
docker push ${DOCKERHUB_USERNAME}/sonar-scanner
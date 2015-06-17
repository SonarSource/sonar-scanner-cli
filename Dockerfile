FROM maven:3.2-jdk-7

RUN apt-get update && apt-get install -y \
  bzip2 \
  vim \
  jq

ENV TESTS SONARQUBE_SNAPSHOT
ENV PATH ~/.local/bin:$PATH

WORKDIR /root
CMD ["./travis.sh"]

ADD . /root/

FROM flink:latest

RUN apt-get update && \
  apt-get install -y maven openjdk-11-jdk vim

ENV JAVA_HOME=/lib/jvm/java-11-openjdk-amd64

WORKDIR /usr/local/flink_app
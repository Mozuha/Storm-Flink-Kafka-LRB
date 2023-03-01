FROM ubuntu:latest

RUN apt-get update && \
  apt-get install -y git openjdk-11-jdk maven python3 wget vim sudo gcc g++ make init systemd

#install storm
WORKDIR /usr/local
RUN wget https://archive.apache.org/dist/storm/apache-storm-2.2.0/apache-storm-2.2.0.tar.gz && \
  tar -zxvf apache-storm-2.2.0.tar.gz && \
  mv apache-storm-2.2.0 storm && \
  rm apache-storm-2.2.0.tar.gz

#setup storm
RUN mkdir /usr/local/storm/data
RUN { \
  echo 'storm.zookeeper.servers:'; \
  echo '  - zookeeper'; \
  echo 'storm.zookeeper.port: 2181'; \
  echo 'nimbus.seeds: ["storm-nimbus"]'; \
  echo 'supervisor.slots.ports:'; \
  echo '  - 6700'; \
  echo '  - 6701'; \
  echo '  - 6702'; \
  echo '  - 6703'; \
  echo 'supervisor.childopts: "-Xmx2G"'; \
  echo 'worker.childopts: "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED -Xmx2G"'; \
  } >> /usr/local/storm/conf/storm.yaml

#symlink python
RUN ln -s /usr/bin/python3 /usr/bin/python

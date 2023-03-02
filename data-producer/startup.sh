#!/bin/sh

#start process and create topic
/usr/local/kafka/bin/kafka-server-start.sh /usr/local/lrb/datadriver/kafka_server.properties &
/usr/local/kafka/bin/kafka-topics.sh --create --topic lrb --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
/bin/bash

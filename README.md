# Storm-Flink-Kafka-LRB

Simple Storm/Flink app that processes Linear Road Benchmark (LRB) events coming from a Kafka producer.

## Objectives

1. Simply consumes incoming events and output them into log files at:  
   i. 1000 events/s  
   ii. 100000 events/s
2. Process toll-notifications query proposed in LRB paper and output query results into log files.

---

## Steps

### 1. Run containers and enter storm nimbus / flink jobmanager container.

Choose either depending on which consumer app you want to run.

```bash
# to run storm app
docker-compose up -d data-producer storm-nimbus storm-supervisor
docker-compose exec storm-nimbus bash

# to run flink app
docker-compose up -d data-producer flink-jobmanager flink-taskmanager
docker-compose exec flink-jobmanager bash
```

### 2a. Run Storm app

One in _storm_app_ directory simply outputs received tuples. One in _toll_notifications_ directory calculates and outputs toll notifications. Run either app.  
[For *storm_app*]

```bash
cd /usr/local/storm_app
mvn clean package -Dstorm.kafka.client.version=3.3.2 -Dcheckstyle.skip

# start storm topology
/usr/local/storm/bin/storm jar target/storm-kafka-lrb-2.2.0.jar main.TollNotifTopology
```

[For *toll_notifications*]

```bash
cd /usr/local/toll_notifications
mvn clean package -Dstorm.kafka.client.version=3.3.2 -Dcheckstyle.skip

# start storm topology
/usr/local/storm/bin/storm jar target/toll-notifications-2.2.0.jar main.KafkaStormTopology
```

### 2b. Run Flink app

One in _flink_app_ directory simply outputs received tuples. One in _toll-notification_app_ directory calculates and outputs toll notifications. Run either app.
[For *flink_app*]

```bash
cd /usr/local/flink_app
mvn clean package -Dcheckstyle.skip

# start flink job
flink run -c main.KafkaFlinkPipeline target/flink-kafka-lrb-1.16.0.jar
```

[For *toll-notification_app*]

```bash
cd /usr/local/toll-notification_app
mvn clean package -Dcheckstyle.skip

# start flink job
flink run -c main.TollNotificationJob target/flink-toll-notifications-1.16.0.jar
```

### 3. Run Kafka producer

Now, Storm/Flink consumer is ready. Run Kafka producer so LRB data are read from file and dispatched to Kafka broker. Consumer app will fetch data from broker as soon as data is received at broker. Then outputs result into log files.

```bash
# in another terminal
docker-compose exec data-producer bash
mvn clean package -Dcheckstyle.skip

# change "storm" to "flink" for flink app
# to run producer at 1000 events/s
java -jar target/kafka-producer-1.0.jar "1000" storm
# to run at 100000 events/s
java -jar target/kafka-producer-1.0.jar "100000" storm
```

In terminal stdout, you will see Kafka metrics _'record-send-rate'_. When I was modifying the parameters of Kafka producer, I relied on this metrics to regard that "certain event rate is achieved". Please note that this metrics value will be varied every time run the producer program. The log files in this repository were taken with ~1000/~100000 events/s which are result of several trial.

Check [here](memo.md#b-observed-producer-metrics-and-valuescodes-used) to see producer parameters used and metrics observed for the log files under tupleoutputs directory.

### 4a. Check Storm logs and stop app

You can find log files under _/storm/supervisor/logs_ . Once you confirm log files are there, you can deactivate/kill Storm topology.  
[For *storm_app*]

```bash
# to deactivate topology
/usr/local/storm/bin/storm deactivate kafka-storm
# to kill topology
/usr/local/storm/bin/storm kill kafka-storm
```

[For *toll_notifications*]

```bash
/usr/local/storm/bin/storm deactivate toll-notification
/usr/local/storm/bin/storm kill toll-notification
```

### 4b. Check Flink logs and stop app

```bash
# to cancel job
flink list  # to get job id
flink cancel <jobid>

# to copy logs from taskmanager container
docker cp flink-taskmanager:/tmp/flink_output /flink/logs
```

### 5. Delete events from Kafka broker

And delete events from Kafka broker. (Do this every time before run consumer app in order to observe behaviour at certain event rate)

```bash
# delete records inside kafka topic
/usr/local/kafka/bin/kafka-delete-records.sh --bootstrap-server=data-producer:9092 --offset-json-file ./offset-file.json

# NOTE: to check what's inside kafka topic
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server=data-producer:9092 --topic lrb --from-beginning
```

Check [here](memo.md#3b-option-2-kafka-connect) for command to delete Kafka topic

---

See [memo](memo.md) for other detailed steps.

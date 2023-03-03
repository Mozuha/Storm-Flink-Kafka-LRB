# Storm, Kafka, Zookeeper, Docker

## 1. Create docker image and container, then start container

```bash
cd example-storm-topologies
docker-compose up -d --build
```

If `docker endpoint for "default" not found` error occurred, delete `meta.json` file under C:/User/Mizuki/.docker/context/meta/{hash}

## 2. Setup and run Data Generator for Linear Road Benchmark

```bash
docker-compose exec mitsimlab bash
```

```bash
yum update -y && yum install -y ld-linux.so.2 wget gcc make postgresql-server perl perl-CPAN perl-Digest-MD5 m4

rm -rf /var/lib/pgsql/data/*
su - postgres
initdb --encoding=UNICODE
^D
echo "listen_addresses='*'" >> /var/lib/pgsql/data/postgresql.conf
echo "host all all 0.0.0.0/0 md5" >> /var/lib/pgsql/data/pg_hba.conf
su - postgres
pg_ctl start  # start postgresql server
psql -c "alter user postgres with encrypted password 'postgres';"
psql -c "create user root with password 'root' superuser;"
psql -c "create database lrb owner root;"
psql -d lrb -c "create schema authorization root;"
psql -c "create role readonly with login password 'readonly';"
^D

perl -MCPAN -e 'install DBI'
perl -MCPAN -e 'install DBD::PgPP'
perl -MCPAN -e 'install Math::Random'

cd usr/local/MITSIMLab
chmod 777 data
wget http://www.cs.brandeis.edu/~linearroad/files/mitsim.tar.gz
tar -zxvf ./mitsim.tar.gz
vi mitsim.config
```

```bash
# mitsim.config
directoryforoutput=/usr/local/MITSIMLab/data
databasename=lrb
databaseusername=root
databasepassword=root
numberofexpressways=2
```

```bash
vi DuplicateCars.pl
```

```bash
# DuplicateCars.pl
70 $dbquery="CREATE TABLE input ( type integer,...);";
↓ change to below
70 $dbquery="CREATE TABLE IF NOT EXISTS input ( type integer,...);";

197 $dbquery="UPDATE input SET carid=carstoreplace.carid WHERE carid=carstoreplace.cartoreplace;";
↓ change to below
197 $dbquery="UPDATE input SET carid=carstoreplace.carid FROM carstoreplace WHERE input.carid=carstoreplace.cartoreplace;";
```

```bash
vi linear-road.pl
```

```bash
# linear-road.pl
use lib '/usr/local/MITSIMLab';  # add this under use strict
```

```bash
mkdir /usr/local/pvm3
cd /usr/local/pvm3
wget https://www.netlib.org/pvm3/pvm3.4.6.tgz
tar -xzvf pvm3.4.6.tgz
vi $HOME/.profile
```

```bash
# .profile
export PVM_ROOT=/usr/local/pvm3/pvm3
export PVM_ALLOW_ROOT=yes
export PATH=$PATH:$PVM_ROOT/bin/LINUX64
```

```bash
source $HOME/.profile
cd /usr/local/pvm3/pvm3
make
```

```bash
mkdir /usr/local/libc6.2.2.so.3
cd /usr/local/libc6.2.2.so.3/
wget https://archives.fedoraproject.org/pub/archive/fedora/linux/releases/16/Everything/x86_64/os/Packages/compat-libstdc++-296-2.96-143.1.i686.rpm
yum localinstall -y compat-libstdc++-296-2.96-143.1.i686.rpm

cd ../MITSIMLab
pvm >> /dev/null
^D
./run mitsim.config
```

## 3. Run kafka producer

```bash
docker-compose up -d zookeeper data-producer
docker-compose exec data-producer bash

# To change advertised listners and zookeeper address, change line 38 and 125 in kafka_server.properties file.

# start kafka broker service in background and create topic lrb
/usr/local/kafka/bin/kafka-server-start.sh /usr/local/lrb/datadriver/kafka_server.properties &
/usr/local/kafka/bin/kafka-topics.sh --create --topic lrb --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1
```

Feed and store whole data into Kafka broker before running Storm app to make it possible simulating certain event rate.

```bash
# produce data (it didn't feed the broker...)
./datafeeder ../dgoutput/cardatapoints.out
# ./datafeeder ../dgoutput/historical-tolls.out

# or via Kafka connect (/usr/local/lrb/kafka_connect)
/usr/local/kafka/bin/connect-standalone.sh connect-standalone.properties connect-file-source.properties


# NOTE: to check what's inside kafka topic
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server=data-producer:9092 --topic lrb --from-beginning

# NOTE: to delete records inside kafka topic
/usr/local/kafka/bin/kafka-delete-records.sh --bootstrap-server=data-producer:9092 --offset-json-file ./offset-file.json

# NOTE: to remove topic
/usr/local/kafka/bin/kafka-server-stop.sh  # stop kafka server
rm -rf /tmp/kafka-logs  # remove topic directory
/usr/local/kafka/bin/zookeeper-shell.sh zookeeper:2181  # connect to zookeeper instance
ls /brokers/topics  # list the topics
deleteall /brokers/topics/lrb  # remove topic folder from zookeeper
^C
/usr/local/kafka/bin/kafka-server-start.sh /usr/local/lrb/datadriver/kafka_server.properties &  # start kafka server again
/usr/local/kafka/bin/kafka-topics.sh --list --bootstrap-server=data-producer:9092  # check if topic was successfully deleted
```

## 4. Start Storm

```bash
# On first terminal (actually, processes are started upon executing container)
docker-compose exec storm-nimbus bash
/usr/local/storm/bin/storm nimbus &
/usr/local/storm/bin/storm ui &  # go to localhost:8081 on browser to check storm ui

# On second terminal (again, processes are started upon executing container)
docker-compose exec storm-supervisor bash
/usr/local/storm/bin/storm supervisor &


# NOTE: disallow writing messages to stdout and allow only write stderr
/usr/local/storm/bin/storm nimbus > /dev/null 2>&1 &
/usr/local/storm/bin/storm supervisor > /dev/null 2>&1 &
/usr/local/storm/bin/storm ui > /dev/null 2>&1 &
```

Access [`http://localhost:8081/`](http://localhost:8081/) to see Storm UI

## 5. Run storm app

```bash
docker-compose exec storm-nimbus bash

# under /usr/local/storm_app
mvn clean package -Dstorm.kafka.client.version=3.3.2 -Dcheckstyle.skip
/usr/local/storm/bin/storm jar target/storm-kafka-lrb-2.2.0.jar main.KafkaStormTopology

# NOTE: to deactivate topology
/usr/local/storm/bin/storm deactivate kafka-storm

# NOTE: to kill topology
/usr/local/storm/bin/storm kill kafka-storm
```

## 6. Extract tuple outputs from log

```bash
# Filter out the logs that matches 'ConsumeBolt' (which shows the tuple output) and then pick those falls into certain timestamp range
# the example below specifies the timestamp range to 52 min 23 sec 468 to 52 min 23 sec 568
sed -n '/ConsumeBolt/p' ../storm/supervisor/logs/kafka-storm-1-1677693130/6700/worker.log | sed -n '/52:23\.468/,/52:23\.568/p' > ./tupleoutputs_1sec.log

# otherwise simply specify interval in seconds
sed -n '/ConsumeBolt/p' ../storm/supervisor/logs/kafka-storm-1-1677693130/6700/worker.log | sed -n '/52:23/,/52:24/p' > ./tupleoutputs_1sec.log
```

## a. Update only one container within same compose

```bash
docker stop {container ID}
docker-compose rm {container ID or name}
docker-compose ps -a  # make sure that the container has removed
docker-compose build [--no-cache] {container name}
docker-compose up -d {container name}
```

## b. LRB tuples details

### `cardatapoints.out`

Consists of **(Type, Time, VID, Spd, Xway, Lane, Dir, Seg, Pos, QID, Sinit, Send, Dow, Tod, Day)** where:

- _Type_: type of tuple (0 = position report
  2 = account balance request
  3 = daily expenditure request
  4 = travel time request)
- _Time_: timestamp that identifies the time at which the position report was emitted (0...10799 second)
- _VID_: vehicle identifier (0...MAXINT)
- _Spd_: speed of the vehicle (0...100)
- _Xway_: express way (0...L-1)
- _Lane_: lane (0...4 where 4 for an exit ramp)
- _Dir_: direction (0...1 where 0 for eastbound and 1 for westbound)
- _Seg_: mile-long segment from which the position report is emitted (0...99)
- _Pos_: position of the vehicle (0...527999)
- _QID_: query identifier
- _Sinit_: start segment
- _Send_: end segment
- _Dow_: day of week (1...7)
- _Tod_: minute number in the day (1...1440)
- _Day_: day where 1 is yesterday and 69 is 10 weeks ago (1...69)

### `historical-tolls.out`

Consists of **(VID, Day, XWay, Tolls)** where:

- _VID_: vehicle identifier (0...MAXINT)
- _Day_: day where 1 is yesterday and 69 is 10 weeks ago (1...69)
- _XWay_: express way (0...L-1)
- _Tolls_: tolls spent on the express way _Xway_ on day _Day_ by vehicle _VID_

By "first query in the article", is it means the "toll notifications" section in [the article](https://www.cs.brandeis.edu/~linearroad/linear-road.pdf)? (p.6)

## c. Simulate various event rate

Set Kafka Consumer config's `max.poll.records` to 1000 and `max.poll.interval.ms` to 1000 in KafkaStormTopology.java to simulate 1000 events/s?  
Then, the number of lines/records extracted to tupleoutputs.log by filtering log output for certain 1 second interval would estimates the throughput at consumer level? (i.e. if there are around 1000 records in tupleoutputs.log, then it suggests that the stream was processed as 1000 events/s in consumer)

---

[Observations]  
_1000 events/s_  
`max.poll.records`: 1000  
`max.poll.interval.ms`: 1000

- tupleoutputs_1000recds.log contains 1021 records
- tupleoutputs_1000recds2.log contains 1292 records (another time period)

_100000 events/s_  
`max.poll.records`: 100000  
`max.poll.interval.ms`: 1000

- tupleoutputs_100000recds.log contains 1897 records
- tupleoutputs_100000recds2.log contains 2102 records (another time period)

---

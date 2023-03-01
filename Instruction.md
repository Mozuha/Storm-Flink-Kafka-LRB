# Storm, Kafka, Zookeeper, Docker

## 1. Create docker image and container, then start container

```bash
cd example-storm-topologies
docker-compose up -d --build
```

If `docker endpoint for "default" not found` error occurred, delete `meta.json` file under C:/User/Mizuki/.docker/context/meta/{hash}

## 2. Download (Setup?) Data Generator for Linear Road Benchmark

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

---

以下 Ubuntu の方でやろうとしてできなかった軌跡

Install PostgreSQL

```bash
apt install -y postgresql
service postgresql start  # start postgresql db
sudo -i -u postgres  # switch user to postgres
createdb lrb  # create DB for Linear Road Benchmark
```

Install MITSIMLab

```bash
# in /usr/local/
mkdir MITSIMLab && cd MITSIMLab/
wget http://www.cs.brandeis.edu/~linearroad/files/mitsim.tar.gz
tar -zxvf ./mitsim.tar.gz
passwd postgres  # set password for the user postgres
mkdir data  # directory where MITSIM data files are to be created
chmod 777 data  # make data directory writable by the user postgres
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
apt install -y libdbi-perl libdbd-pg-perl libpq-dev pvm gcc-multilib
perl -MCPAN -e shell
install DBD::PgPP
install Math::Random
exit
./run mitsim.config
```

ln647 2 -> 3

/etc/postgresql/14/main/postgresql.conf

vi /etc/postgresql/14/main/postgresql.conf

```bash
unix_socket_directories = '/var/run/postgresql'
↓ change to below
unix_socket_directories = '/tmp'
```

```bash
vi DuplicateCars.pl
```

```bash
# DuplicateCars.pl
64 my $dbh = DBI->connect("DBI:PgPP:$dbname", $dbuser, $dbpassword)
↓ change to below
64 my $dbh = DBI->connect("DBI:Pg:dbname=$dbname", $dbuser, $dbpassword)

70 $dbquery="CREATE TABLE input ( type integer,...);";
↓ change to below
70 $dbquery="CREATE TABLE IF NOT EXISTS input ( type integer,...);";
```

systemctl restart postgresql

```bash
vi linear-road.pl
```

```bash
# linear-road.pl
use lib '/usr/local/MITSIMLab';  # add this under use strict
```

```bash
export PVM_ALLOW_ROOT=yes  # to enable PVM to be runnable as user "root"
```

export PVM_ROOT=$MITSIMDIR/PVM/pvm
export PATH=$PATH:$MITSIMDIR/bin:$PVM_ROOT/lib:$PVM_ROOT/bin/LINUX

```bash
# .profile
export MITSIMDIR=/usr/local/MITSIMLab
export PVM_ROOT=$MITSIMDIR/MITSIMLab/PVM/pvm
export PATH=$PATH:/usr/local/storm/bin:$MITSIMDIR/MITSIMLab/bin:$PVM_ROOT/bin/LINUX
```

/usr/local/MITSIMLab/MITSIMLab/bin/mitsim

/usr/local/MITSIMLab/MITSIMLab/PVM/pvm/lib/pvm

---

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
```

## 4. Start Storm

```bash
# On first terminal
docker-compose exec storm-nimbus bash
/usr/local/storm/bin/storm nimbus &
/usr/local/storm/bin/storm ui &  # go to localhost:8081 on browser to check storm ui

# On second terminal
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

# NOTE: to kill topology
/usr/local/storm/bin/storm kill kafka-storm
```

## 6. Extract tuple outputs from log

```bash
# Filter out the logs that matches 'ConsumeBolt' (which shows the tuple output) and then pick those falls into certain timestamp range
# the example below specifies the timestamp range to 52 min 23 sec 468 to 52 min 23 sec 568
sed -n '/ConsumeBolt/p' ../storm/supervisor/logs/kafka-s
torm-1-1677693130/6700/worker.log | sed -n '/52:23\.468/,/52:23\.568/p' > ./tupleoutputs_1sec.log
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
- _Lane_: lane (0...4)
- _Dir_: direction (0...1)
- _Seg_: segment (0...99)
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

Set `fetch.min.bytes` config of Kafka Consumer config to 1000 to simulate 1000 events/s for 1 second?

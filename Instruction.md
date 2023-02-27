# Storm, Kafka, Zookeeper, Docker

### 1. Create docker image and container, then start container

```bash
cd example-storm-topologies
docker-compose up -d --build
```

If `docker endpoint for "default" not found` error occurred, delete `meta.json` file under C:/User/Mizuki/.docker/context/meta/{hash}

### 2. Get into container

```bash
docker-compose exec main bash
```

### 3. Download Zookeeper

```bash
cd usr/local/
wget https://dlcdn.apache.org/zookeeper/zookeeper-3.7.1/apache-zookeeper-3.7.1-bin.tar.gz
tar -zxvf apache-zookeeper-3.7.1-bin.tar.gz
mv apache-zookeeper-3.7.1-bin zookeeper  # rename directory
```

### 4. Setup Zookeeper

```bash
mkdir /usr/local/zookeeper/data/
vi /usr/local/zookeeper/conf/zoo.cfg
```

```bash
# zoo.cfg
tickTime=2000
dataDir=/usr/local/zookeeper/data
clientPort=2181
initLimit=10
syncLimit=5
```

### 5. Start Zookeeper

```bash
/usr/local/zookeeper/bin/zkServer.sh start
```

### 6. Download Storm

```bash
# in /usr/local/
wget https://archive.apache.org/dist/storm/apache-storm-2.2.0/apache-storm-2.2.0.tar.gz
tar -zxvf apache-storm-2.2.0.tar.gz
mv apache-storm-2.2.0 storm
```

### 7. Setup Storm

```bash
mkdir /usr/local/storm/data
vi /usr/local/storm/conf/storm.yaml
```

```bash
# storm.yaml
storm.zookeeper.servers:
     - "localhost"
storm.local.dir: "/usr/local/storm/data"
nimbus.seeds: ["localhost"]
supervisor.slots.ports:
     - 6700
     - 6701
     - 6702
     - 6703
ui.port: 8081
```

```bash
vi $HOME/.profile
```

```bash
# .profile
export PATH=$PATH:/usr/local/storm/bin  # set path
```

```bash
source ~/.profile  # immediately apply path setting
ln -s /usr/bin/python3 /usr/bin/python  # create symlink to python3 bin
```

### 8. Start Storm

```bash
storm nimbus &  # run in background
storm supervisor &
storm ui &
```

Access [`http://localhost:8081/`](http://localhost:8081/) to see Storm UI

### 9. Download Kafka

```bash
wget https://downloads.apache.org/kafka/3.3.2/kafka_2.12-3.3.2.tgz
tar -zxvf kafka_2.12-3.3.2.tgz
mv kafka_2.12-3.3.2 kafka
```

### 10. Download (Setup?) Data Generator for Linear Road Benchmark

Get into CentOS7 container and generate data

mitsim コマンドが Ubuntu22 だと動かないっぽい？CentOS7 なら動くかも？

別の container を CentOS7 で作って MITSIMLab の Data Generator だけそこで走らせて、output data を volumes に永続化してそれを流用？

```bash
docker-compose exec mitsimlab bash
```

```bash
# compat-libstdc++-33
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
vi [DuplicateCars.pl](http://duplicatecars.pl/)
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
vi [DuplicateCars.pl](http://duplicatecars.pl/)
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

### 11. Set up Kafka producer

```bash
docker-compose up -d zookeeper main
docker-compose exec main bash

# To change advertised listners and zookeeper address, change line 38 and 125 in kafka_server.properties file.

# produce data
./datafeeder ../dgoutput/historical-tolls.out
```

### a. Update only one container within same compose

```bash
docker stop {container ID}
docker-compose rm {container ID or name}
docker-compose ps -a  # make sure that the container has removed
docker-compose build [--no-cache] {container name}
docker-compose up -d {container name}
```

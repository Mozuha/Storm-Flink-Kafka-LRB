#!/bin/sh

#termination process
handle(){
  /usr/local/storm kill_workers
  /etc/init.d/postgresql stop
  exit 0
}
trap handle TERM INT

#start process
/sbin/init
/usr/local/zookeeper/bin/zkServer.sh start
storm nimbus &
storm supervisor &
storm ui &
/etc/init.d/postgresql start
/bin/bash

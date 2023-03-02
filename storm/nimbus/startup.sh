#!/bin/sh

#start process
/usr/local/storm/bin/storm nimbus > /dev/null 2>&1 &
/usr/local/storm/bin/storm ui > /dev/null 2>&1 &
/bin/bash

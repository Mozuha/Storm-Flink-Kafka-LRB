#!/bin/sh

#start process
/usr/local/storm/bin/storm supervisor > /dev/null 2>&1 &
/bin/bash

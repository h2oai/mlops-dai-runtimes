#!/usr/bin/env bash

memTotalKb=`cat /proc/meminfo | grep MemTotal | sed 's/MemTotal:[ \t]*//' | sed 's/ kB//'`
xmxMb=$[ $memTotalKb / 1024 * 90 / 100 ]

FILE="/tmp/pipeline.mojo"
while [ ! -f $FILE ]
do
  echo "File [$FILE] does not exist.  Retrying in 5 seconds ..."
  sleep 5
done

java -Xmx${xmxMb}m -Dmojo.path=$FILE -jar /opt/h2oai/mojo-scorer/local-rest-scorer.jar

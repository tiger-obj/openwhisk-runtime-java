#!/bin/bash

export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0
export WSK_CONFIG_FILE=/home/rbruno/git/incubator-openwhisk-devtools/docker-compose/.wskprops
export OPENWHISK_HOME=/home/rbruno/git/incubator-openwhisk-devtools/docker-compose/openwhisk-src/
wsk=$OPENWHISK_HOME/bin/wsk

$wsk action create -i Sort sort.jar --main Sort --docker rfbpb/java8action -c 100
#$wsk action update -i Sort sort.jar --main Sort --docker rfbpb/java8action -c 100

#!/bin/bash

export WSK_CONFIG_FILE=/home/rbruno/git/incubator-openwhisk-devtools/docker-compose/.wskprops
export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0
export OPENWHISK_HOME=/home/rbruno/git/openwhisk

wsk action update -i Hello hello.jar --main Hello --docker rfbpb/java8action

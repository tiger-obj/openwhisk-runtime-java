#!/bin/bash

export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0

debug="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y"

$JAVA_HOME/bin/java $debug -Dfile.encoding=UTF-8 -jar core/java8/proxy/build/libs/proxy-all.jar

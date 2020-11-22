#!/bin/bash

export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -jar /home/sutwang/Project/openwhisk-runtime-java/core/java8/proxy/build/libs/proxy-all.jar 2>&1 | tee proxy.log

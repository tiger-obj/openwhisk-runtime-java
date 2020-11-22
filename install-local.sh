#!/bin/bash

export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0

cd core/java8/proxy
./gradlew oneJar
cd -

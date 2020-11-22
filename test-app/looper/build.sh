#!/bin/bash

export JAVA_HOME=/home/sutwang/software/graalvm-ce-java8-20.2.0

$JAVA_HOME/bin/javac -cp ../../gson-2.8.5.jar Looper.java
$JAVA_HOME/bin/jar cvf looper.jar Looper.class

#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/sleep"
python3 $PROJECTPATH/tools/invoke.py init ch.ethz.systems.Sleep $PROJECTPATH/$SUBFOLDERPATH/target/sleep.jar

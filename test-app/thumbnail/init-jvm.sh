#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/thumbnail"
python3 $PROJECTPATH/tools/invoke.py init ch.ethz.systems.Thumbnail $PROJECTPATH/$SUBFOLDERPATH/target/thumbnail.jar

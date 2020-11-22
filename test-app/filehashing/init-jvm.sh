#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/filehashing"
python3 $PROJECTPATH/tools/invoke.py init ch.ethz.systems.FileHashing $PROJECTPATH/$SUBFOLDERPATH/target/filehashing.jar

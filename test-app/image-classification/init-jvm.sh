#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/image-classification"
python3 $PROJECTPATH/tools/invoke.py init ch.ethz.systems.InceptionImageClassifierDemo $PROJECTPATH/$SUBFOLDERPATH/target/image-classifier.jar

#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/thumbnail"
python3 $PROJECTPATH/tools/invoke.py run '{"seed":"1"}'

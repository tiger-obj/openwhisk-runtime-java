#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/sleep"
python3 $PROJECTPATH/tools/invoke.py run '{"time":"0"}'

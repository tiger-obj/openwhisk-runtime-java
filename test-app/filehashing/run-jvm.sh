#!/bin/bash
PROJECTPATH="/home/sutwang/Project/openwhisk-runtime-java"
SUBFOLDERPATH="test-app/filehashing"
python3 $PROJECTPATH/tools/invoke.py run '{"seed":"35"}'

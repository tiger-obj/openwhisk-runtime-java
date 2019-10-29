#!/bin/bash

# syntax: experiment-run.sh /home/rbruno/git/openwhisk-runtime-java/test-app/sort

app_dir=$1
wrk_dir=$app_dir/results

source $app_dir/experiment.config

function run {
    # step 1: launch docker
    docker run -m $mem --cpus=$vcpu -p 8080:8080 --name $app_name rfbpb/java8action &> container.log &

    # just let docker setup...
    sleep 5

    # step 2: build benchmark (generate jar file) and upload it (init proxy)
    cd $app_dir; ./build.sh; ./init-jvm.sh; cd - &> /dev/null

    # step 3: run loadrunner
    cd tools; ./loadrunner-jvm.py -nt $cthread -ne $nexps -nw $nwarms | tee $wrk_dir/$tag.log | grep -v "#"; cd - &> /dev/null

    # step 4: kill docker
    docker kill $app_name
    docker rm $app_name
}

mkdir $wrk_dir &> /dev/null
rm $wrk_dir/*
docker pull rfbpb/java8action

for cthread in $cthreads
do
    if   [ "$cthread" = "1"  ]; then vcpu="0.1"; mem="192m"
    elif [ "$cthread" = "2"  ]; then vcpu="0.2"; mem="384m"
    elif [ "$cthread" = "4"  ]; then vcpu="0.4"; mem="768m"
    elif [ "$cthread" = "6"  ]; then vcpu="0.6"; mem="1088m"
    elif [ "$cthread" = "8"  ]; then vcpu="0.8"; mem="1472m"
    elif [ "$cthread" = "10" ]; then vcpu="1";   mem="1792m"
    elif [ "$cthread" = "12" ]; then vcpu="1.2"; mem="2176m"
    elif [ "$cthread" = "14" ]; then vcpu="1.4"; mem="2560m"
    elif [ "$cthread" = "16" ]; then vcpu="1.6"; mem="2880m"
    else echo "Unknown vcpu config..."; exit 1
    fi
    for rep in $reps
    do
        tag="vcpu-$vcpu-conc-$cthread-rep-$rep"
        echo "running $wrk_dir/$tag.log ..."
        #run
        echo "running $wrk_dir/$tag.log ... done"
    done
done
#!/bin/sh

JGRAPES_DIST=../grapes/jgrapes/dist/

LOG_DIRECTORY=~/pushes-out/
IP=192.168.0.4
BASEPORT=20000
CLOUD_PROVIDER=mysql
CLOUD_URI=mysql://nivox@localhost/cloud/delays
NETSIZE=64
DURATION=60
ITERATIONS=2
NEWS_PERIOD=5
AE_PERIOD=10
RM_PERIOD=1
NEWS_CONTROLLER=true
#============================================================
# DON'T EDIT PAST THIS UNLESS YOU KNOW WHAT YOU'RE DOING
#============================================================

BUILD_DIR="./build"
LIB_DIRS="./lib"

CLASSPATH="$CLASSPATH:$BUILD_DIR"
for dir in $LIB_DIRS; do
    for jar in `ls $dir/*.jar`; do
        CLASSPATH+=:$jar
    done
done

JVMARGS="-Djava.library.path=$JGRAPES_DIST"
export LD_LIBRARY_PATH+=:$JGRAPES_DIST

ulimit -a
java -Xms32m -Xmx1024m  -cp $CLASSPATH $JVMARGS statistics.Statistics $LOG_DIRECTORY $IP $BASEPORT $CLOUD_PROVIDER \
    $CLOUD_URI $NETSIZE $DURATION $ITERATIONS $NEWS_PERIOD $AE_PERIOD $RM_PERIOD $NEWS_CONTROLLER
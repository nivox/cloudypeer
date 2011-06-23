#!/bin/sh

# Run script for a simple test application that uses CloudyPeer to
# implement a really basic news diffusion.
#
# Start the application this way:
# $ ./run-simple-app.sh <ip> <port>
#
# where <ip> is the ip address of the local node and <port> the
# corresponding port number. (Keep in mind that it is possible that
# multiple port number will be bound, for instance the cloudcast
# implementation based on jgrapes will bound udp port <port>+1).
#
# NOTE: to successfully start the application you need to customize
# the following variables:

# Cloud provider implementation: mysql, amazons3
CLOUD_PROVIDER=mysql

# URI of the cloud used for the peer sampling
# i.e. PS_CLOUD="mysql://user:pass@localhost/database/bucket/key"
PS_CLOUD=mysql://nivox@localhost/cloud/testbucket/view

# URI of the cloud used for the app store
# i.e. PS_CLOUD="mysql://user:pass@localhost/database/bucket"
STORE_CLOUD=mysql://nivox@localhost/cloud/testbucket

# Path of the directory holding the jgrapes distribution (native libraries)
JGRAPES_DIST=../grapes/jgrapes/dist/

#============================================================
# DON'T EDIT PAST THIS UNLESS YOU KNOW WHAT YOU'RE DOING
#============================================================

LOCAL_IP=$1
LOCAL_PORT=$2

BUILD_DIR="./build"
LIB_DIRS="./lib"
if [ -z "$CLOUD_PROVIDER" -o -z "$PS_CLOUD" -o -z "$STORE_CLOUD" -o -z "$JGRAPES_DIST" ]; then
    echo "Missing configuration... Read the header of this script for help"
    exit 1
fi

if [ -z "$LOCAL_IP" -o -z "$LOCAL_PORT" ]; then
    echo "Missing command line arguments: <ip> <port>"
    exit 1
fi

CLASSPATH="$CLASSPATH:$BUILD_DIR"
for dir in $LIB_DIRS; do
    for jar in `ls $dir/*.jar`; do
        CLASSPATH+=:$jar
    done
done

JVMARGS="-Djava.library.path=$JGRAPES_DIST"
export LD_LIBRARY_PATH+=:$JGRAPES_DIST
java -cp $CLASSPATH $JVMARGS test.simple.SimpleApp $1 $2 $CLOUD_PROVIDER $PS_CLOUD $STORE_CLOUD
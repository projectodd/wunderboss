#!/bin/bash

# This is the bees CI build. Any changes to the build script should be
# here instead if in the bees config.

set -e

DIR=$( cd "$( dirname "$0" )" && pwd )

function mark {
    echo
    echo "=============================================="
    echo $1
    date
    echo "=============================================="
    echo
}

export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"

mark "Starting build script"
java -version
mvn -version
git clean -fd

mark "Cleaning"
mvn -B clean

mark "Reversioning"
mvn -B versions:set -DnewVersion=1.x.incremental.${BUILD_NUMBER}

mark "Building"
mvn -B -s ${SETTINGS_FILE} install

mark "Testing messaging against HornetQ 2.3"
mvn -B -s ${SETTINGS_FILE} -f modules/messaging-hornetq/pom.xml -P hornetq-2.3 test

mark "Deploying"
mvn -B -s ${SETTINGS_FILE} -Pbees deploy

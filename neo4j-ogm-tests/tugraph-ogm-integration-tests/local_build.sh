#!/usr/bin/env bash

# build tugraph-ogm-integration-tests
echo build tugraph-ogm-integration-tests
mvn clean install

# build tugraph-ogm
echo build tugraph-ogm
ogmpath=$(dirname $(dirname "$PWD"))
echo $ogmpath
cd $ogmpath
mvn clean install -DskipTests -Denforcer.skip=true

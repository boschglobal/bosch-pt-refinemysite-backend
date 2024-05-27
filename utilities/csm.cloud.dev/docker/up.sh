#!/bin/bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

docker-compose -f "${SCRIPT_DIR}/docker-compose.yml" up -d \
 mysql \
 mongodb \
 mongodb-init \
 broker schema-registry \
 storage-emulator

sleep 20
docker-compose rm -f mongodb-init
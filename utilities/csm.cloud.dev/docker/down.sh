#!/bin/bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

docker-compose -f ${SCRIPT_DIR}/docker-compose.yml down

docker volume prune -f
docker system prune -f
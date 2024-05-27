#!/bin/bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd $SCRIPT_DIR/..

# Download public and private dependencies utilizing git authentication features
go env -w GOPRIVATE="dev.azure.com/pt-iot/smartsite/*"
go env -w GOMODCACHE="$(pwd)/.gomodcache"
go mod download

docker build -t ptcsmacr.azurecr.io/com.bosch.pt/csm.cloud.image.scale -f Dockerfile .

cd -

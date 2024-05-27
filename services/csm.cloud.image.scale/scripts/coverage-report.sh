#!/bin/bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd $SCRIPT_DIR/..

go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out -o coverage.html

cd -

#!/bin/bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd $SCRIPT_DIR/..

# Install Nancy: https://golangexample.com/a-tool-to-check-for-vulnerabilities-in-your-golang-dependencies/
go list -json -m all | nancy sleuth

cd -

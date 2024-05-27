#!/bin/bash

DEP_CHECK_VERSION="$1"

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd $SCRIPT_DIR/..

# Download OWASP Dependency-Check only if version is provided
if [ ${DEP_CHECK_VERSION} ]; then
    echo "Downloading OWASP Dependency-Check ${DEP_CHECK_VERSION}."
    curl -LO https://github.com/jeremylong/DependencyCheck/releases/download/v${DEP_CHECK_VERSION}/dependency-check-${DEP_CHECK_VERSION}-release.zip
    unzip dependency-check-${DEP_CHECK_VERSION}-release.zip
else
    echo "Skipping download OWASP Dependency-Check."
fi

./dependency-check/bin/dependency-check.sh \
    --data .owasp-nvd \
    --enableExperimental \
    --project csm.cloud.image.scale \
    --scan .

cd -

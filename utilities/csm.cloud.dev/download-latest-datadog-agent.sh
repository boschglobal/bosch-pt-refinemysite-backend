#!/bin/bash

set -e

if [ ! -d ../datadog ]; then
    mkdir -p ../datadog;
fi;

curl https://dtdg.co/latest-java-tracer --output ../datadog/dd-java-agent.jar --location

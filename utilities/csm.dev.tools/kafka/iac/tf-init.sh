#!/bin/bash

USAGE=$"
-e:env[The environment for which the plan will be executed.]{sandbox1-4|dev|review|test1|prod1}
-s:subscription[The subscription for which the plan will be executed.]{sandbox|dev|qa|prod}
"

while getopts ":e:s:" OPT; do
    case ${OPT} in
        e)
            ENV="${OPTARG}"
            ;;
        s)
            SUBSCRIPTION="${OPTARG}"
            ;;
        ?)
            echo "Invalid option: ${OPTARG}"
            echo "Usage: ${USAGE}"
            exit 1
            ;;
    esac
done

if [[ -z ${ENV} ]]; then
    echo "Failed to extract environment.";
    exit 2;
fi

if [[ -z ${SUBSCRIPTION} ]]; then
    echo "Failed to extract subscription.";
    exit 2;
fi

BACKEND_CONFIG=~/.tf-config/backend-${SUBSCRIPTION}.tfvars;
if [[ ! -f ${BACKEND_CONFIG} ]]; then
    echo "Backend configuration ${BACKEND_CONFIG} does not exist.";
    exit 2;
fi

ENV_CONFIG=config/${ENV}.tfvars;
if [[ ! -f ${ENV_CONFIG} ]]; then
    echo "Environment configuration ${ENV_CONFIG} does not exist.";
    exit 2;
fi

# Workaround when switching the environment. Remove local directory .terraform before initialization of a new backend.
if [ -d ".terraform" ]; then
    echo "Remove .terraform before initialization.";
    rm -rf .terraform;
fi

# Use option -upgrade to rewrite .terraform.lock.hcl file
terraform init -upgrade -backend-config=${BACKEND_CONFIG} -backend-config="key=${ENV}-confluent.tfstate";
# Generate checksum for macOS and Linux
terraform providers lock -platform=darwin_amd64 -platform=linux_amd64;

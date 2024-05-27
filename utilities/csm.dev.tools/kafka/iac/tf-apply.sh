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

CONFLUENT_CLOUD_API_KEY=~/.tf-config/confluentcloud.tfvars;
if [[ ! -f ${CONFLUENT_CLOUD_API_KEY} ]]; then
    echo "Confluent Cloud API Key ${CONFLUENT_CLOUD_API_KEY} does not exist.";
    exit 2;
fi

SP_CONFIG=~/.tf-config/sp-${SUBSCRIPTION}.tfvars;
if [[ ! -f ${SP_CONFIG} ]]; then
    echo "SP configuration ${SP_CONFIG} does not exist.";
    exit 2;
fi

ENV_CONFIG=config/${ENV}.tfvars;
if [[ ! -f ${ENV_CONFIG} ]]; then
    echo "Environment configuration ${ENV_CONFIG} does not exist.";
    exit 2;
fi

terraform apply -var-file=${CONFLUENT_CLOUD_API_KEY} -var-file=${SP_CONFIG} -var-file=${ENV_CONFIG};

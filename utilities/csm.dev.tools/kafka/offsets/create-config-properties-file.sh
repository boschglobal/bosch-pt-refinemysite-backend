#!/bin/bash

# This script logs you in using the confluent CLI and creates the config.properties 
# file holding the login configuration and credentials for the selected Kafka cluster.
#
# Therafter, you can use that file with Kafka CLI tools such as kafka-consumer-groups.

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_kafka.sh"

init_environment_and_color_from_args $@

azure_login
az account set --subscription "${SUBSCRIPTION}"
init_kafka_key_vault $ENVIRONMENT
kafka_connect

# Get service account and api key
SERVICE_NAME="read-only"
SERVICE_NAME_SHORT="readonly"
find_service_account_id "csm-${ENV_SHORT}-${SERVICE_NAME_SHORT}-${BLUE_GREEN_SHORT}"
API_KEY=$(read_secret "${VAULT_NAME}" "csm-${SERVICE_NAME}-kafka-broker-api-key-${BLUE_GREEN}")
API_SECRET=$(read_secret "${VAULT_NAME}" "csm-${SERVICE_NAME}-kafka-broker-api-secret-${BLUE_GREEN}")

# Get cluster endpoint URL
ENDPOINT_URL=$(kafka_cluster_endpoint_url_get)
echo "Using endpoint $ENDPOINT_URL"

# Generate properties file to be used by kafka-consumer-group command-line-utility
create_kafka_config_properties_file "config.properties" "${API_KEY}" "${API_SECRET}"

echo "Created config.properties"
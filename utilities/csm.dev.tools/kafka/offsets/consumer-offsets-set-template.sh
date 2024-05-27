#!/bin/bash

# This script is meant as a starting point to reset consumer group offsets.
# 
# You can add all necessary commands at the bottom of this script.

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_kafka.sh"

init_environment_and_color_from_args $@

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

# Generate properties file to be used by kafka-consumer-group command-line-utility
create_kafka_config_properties_file "config.properties" "${API_KEY}" "${API_SECRET}"

confirm "Continue? (y/n) "

echo
kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --command-config config.properties --group="<consumer-group>" --topic="<topic>" --reset-offsets --to-earliest --execute
# Add more commands here, if you wish

echo "Done"

#!/bin/bash

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_kafka.sh"

init_environment_and_color_from_args $@

init_env_key_vault $ENVIRONMENT

KAFKA_TOPIC_COMPANY=$(read_secret "${VAULT_NAME}" "kafka-topic-company")
KAFKA_TOPIC_CRAFT=$(read_secret "${VAULT_NAME}" "kafka-topic-craft")
KAFKA_TOPIC_USER=$(read_secret "${VAULT_NAME}" "kafka-topic-user")
KAFKA_TOPIC_PROJECT=$(read_secret "${VAULT_NAME}" "kafka-topic-project")
echo
echo "Using the following topics:"
echo "Company topic: ${KAFKA_TOPIC_COMPANY}"
echo "Craft topic: ${KAFKA_TOPIC_CRAFT}"
echo "User topic: ${KAFKA_TOPIC_USER}"
echo "Project topic: ${KAFKA_TOPIC_PROJECT}"

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
echo "Set offsets of csm.cloud.kafka.migrator (csm-migrator-${ENV})"
kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --group="csm-migrator-${ENV}" --command-config config.properties --topic=${KAFKA_TOPIC_COMPANY} --reset-offsets --to-earliest --execute
kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --group="csm-migrator-${ENV}" --command-config config.properties --topic=${KAFKA_TOPIC_CRAFT} --reset-offsets --to-earliest --execute
kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --group="csm-migrator-${ENV}" --command-config config.properties --topic=${KAFKA_TOPIC_USER} --reset-offsets --to-earliest --execute
kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --group="csm-migrator-${ENV}" --command-config config.properties --topic=${KAFKA_TOPIC_PROJECT} --reset-offsets --to-earliest --execute

echo "Done"

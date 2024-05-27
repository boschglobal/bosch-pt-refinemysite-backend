#!/bin/bash

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
SERVICE_NAME="dev-del-cgp"
SERVICE_NAME_SHORT="del-cgp"
find_service_account_id "csm-${ENV_SHORT}-${SERVICE_NAME_SHORT}-${BLUE_GREEN_SHORT}"
API_KEY=$(read_secret "${VAULT_NAME}" "csm-${SERVICE_NAME}-kafka-broker-api-key-${BLUE_GREEN}")
API_SECRET=$(read_secret "${VAULT_NAME}" "csm-${SERVICE_NAME}-kafka-broker-api-secret-${BLUE_GREEN}")

# Get cluster endpoint URL
ENDPOINT_URL=$(kafka_cluster_endpoint_url_get)

# Generate properties file to be used by kafka-consumer-group command-line-utility
create_kafka_config_properties_file "config.properties" "${API_KEY}" "${API_SECRET}"

# Get the consumer groups
CONSUMER_GROUPS=$(kafka-consumer-groups --bootstrap-server="$ENDPOINT_URL" --command-config config.properties --timeout 30000 --list)

echo
echo "The following consumer groups are available on the cluster:"
echo "${CONSUMER_GROUPS}"

echo
echo "Select consumer groups to delete:"

declare -a SELECTED_CONSUMER_GROUPS=()
while true; do
  read -p "Enter a consumer group name (or leave empty to stop and confirm by pressing enter): " ENTERED_CONSUMER_GROUP
  if [[ $ENTERED_CONSUMER_GROUP == "" ]]; then
    break
  else
    SELECTED_CONSUMER_GROUPS+=("$ENTERED_CONSUMER_GROUP")
  fi
done

confirm 'Continue deleting consumer-groups? (y/n) '

for CONSUMER_GROUP in ${SELECTED_CONSUMER_GROUPS[@]}; do
  echo
  echo "Delete consumer-group: ${CONSUMER_GROUP}"
  kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --group=$CONSUMER_GROUP --command-config config.properties --delete
done

echo
echo "Done"


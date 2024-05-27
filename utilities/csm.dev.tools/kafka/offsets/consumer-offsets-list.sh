#!/bin/bash

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_kafka.sh"

init_environment_and_color_from_args $@
kafka_connect
init_kafka_key_vault $ENVIRONMENT

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

echo
echo "The following consumer groups are available:"
CONSUMER_GROUPS=$(kafka-consumer-groups --timeout 15000 --bootstrap-server="$ENDPOINT_URL" --command-config config.properties --list | grep "csm.*${ENV}")
echo "${CONSUMER_GROUPS}"

echo
read -p "Display offsets of a (s)ubset or (a)ll consumer groups? " SUBSET_ALL
if [[ $SUBSET_ALL == "s" ]]; then

  # Enter consumer group names
  declare -a SELECTED_CONSUMER_GROUPS=()
  while true; do
    read -p "Enter a consumer group name (or leave empty to stop and confirm by pressing enter): " ENTERED_CONSUMER_GROUP
    if [[ $ENTERED_CONSUMER_GROUP == "" ]]; then
      break
    else
      SELECTED_CONSUMER_GROUPS+=("$ENTERED_CONSUMER_GROUP")
    fi
  done

  # Get offsets
  for CONSUMER_GROUP in ${SELECTED_CONSUMER_GROUPS[@]}; do
    echo
    echo "Get offset of consumer-group: ${CONSUMER_GROUP}"
    kafka-consumer-groups --timeout 15000 --bootstrap-server=$ENDPOINT_URL --group=$CONSUMER_GROUP --command-config config.properties --describe
  done

else
  for CONSUMER_GROUP in $CONSUMER_GROUPS
  do
    echo
    echo "Get offsets for consumer group: $CONSUMER_GROUP"
    kafka-consumer-groups --timeout 15000 --bootstrap-server=$ENDPOINT_URL --group=$CONSUMER_GROUP --command-config config.properties --describe
  done
fi

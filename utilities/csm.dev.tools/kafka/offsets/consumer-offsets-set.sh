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

# Get the consumer groups
CONSUMER_GROUPS=$(kafka-consumer-groups --timeout 15000 --bootstrap-server="$ENDPOINT_URL" --command-config config.properties --list | grep "csm.*${ENV}")

while true; do
  echo
  echo "The following topics are available on the cluster:"
  CLUSTER_TOPICS=$(confluent kafka topic list | awk '/csm.*/{print $1}' | grep "csm.*${ENV}")
  echo "${CLUSTER_TOPICS}"

  read -p "Enter a topic name you want to set the offsets of: " OFFSET_TOPIC
  if [[ ${CLUSTER_TOPICS} != *"$OFFSET_TOPIC"* ]]; then
    echo "Invalid topic name entered"
    continue
  fi

  echo
  echo "Analyzing existing consumer-groups..."
  CONSUMER_GROUPS_OF_TOPIC=$(kafka-consumer-groups --bootstrap-server=$ENDPOINT_URL --command-config config.properties --timeout 30000 \
                                                   --describe --all-groups | jq -sR --arg TOPIC "$OFFSET_TOPIC" \
      '[. | splits("\n")                          # Split by line breaks
          | select(length>0)                      # Remove empty lines
          | select(contains("GROUP") | not)       # Remove table headers
          | [ splits(" +") ]                      # Split each line by whitespaces
          | {group: .[0], topic: .[1]}            # Construct JSON object {group: ..., topic: ...}
          | select(.topic==$TOPIC)                # Filter consumer groups of topic
          | .group
        ] | unique                                # Return unique consumer group names
      ')

  # Create array from $CONSUMER_GROUPS_OF_TOPIC
  # sources: https://stackoverflow.com/a/35005983/7492402
  #          https://unix.stackexchange.com/a/99427
  CONSUMER_GROUPS_OF_TOPIC_ARRAY=()
  while read GROUP; do
    CONSUMER_GROUPS_OF_TOPIC_ARRAY+=( $GROUP )
  done < <(echo $CONSUMER_GROUPS_OF_TOPIC | jq -cr '.[]')

  echo
  echo "The following consumer groups are available on the cluster:"
  echo "${CONSUMER_GROUPS}"

  echo
  echo "The following consumer groups have an offset for the topic ${OFFSET_TOPIC}:"
  printf '%s\n' "${CONSUMER_GROUPS_OF_TOPIC_ARRAY[@]}"

  echo
  while true; do
    read -p "Do you want to set offsets of (a)ll consumers having already an offset or (m)anually enter consumer group names? " ALL_OR_MANUALLY
    if [[ $ALL_OR_MANUALLY == "a" || $ALL_OR_MANUALLY == "m" ]]; then
      break
    fi
  done

  echo
  while true; do
    read -p "Set offsets to (e)arliest or to (l)atest? " EARLIEST_OR_LATEST
    case $EARLIEST_OR_LATEST in
      "e" ) EARLIEST_LATEST="--to-earliest"; break;;
      "l" ) EARLIEST_LATEST="--to-latest"; break;;
      * ) echo "Invalid input.";;
    esac
  done

  declare -a SELECTED_CONSUMER_GROUPS=()
  if [[ $ALL_OR_MANUALLY == "m" ]]; then
    # Enter consumer group names
    while true; do
      read -p "Enter a consumer group name (or leave empty to stop and confirm by pressing enter): " ENTERED_CONSUMER_GROUP
      if [[ $ENTERED_CONSUMER_GROUP == "" ]]; then
        break
      else
        SELECTED_CONSUMER_GROUPS+=("$ENTERED_CONSUMER_GROUP")
      fi
    done

    # Set offsets
    for CONSUMER_GROUP in ${SELECTED_CONSUMER_GROUPS[@]}; do
      echo
      echo "Set offset of consumer-group: ${CONSUMER_GROUP}"
      kafka-consumer-groups --timeout 15000 --bootstrap-server=$ENDPOINT_URL --group=$CONSUMER_GROUP --command-config config.properties --topic=$OFFSET_TOPIC --reset-offsets ${EARLIEST_LATEST} --execute
    done

  else
    # Set offsets for all consumers already having an offset
    for CONSUMER_GROUP in ${CONSUMER_GROUPS_OF_TOPIC_ARRAY[@]}; do
      echo
      echo "Set offset of consumer-group: ${CONSUMER_GROUP}"
      kafka-consumer-groups --timeout 15000 --bootstrap-server=$ENDPOINT_URL --group=$CONSUMER_GROUP --command-config config.properties --topic=$OFFSET_TOPIC --reset-offsets ${EARLIEST_LATEST} --execute
    done
  fi

  echo
  read -p "Set offset of (a)nother topic or (s)top? " ANOTHER_OR_STOP
  if [[ $ANOTHER_OR_STOP != "a" ]]; then
    break
  fi
done

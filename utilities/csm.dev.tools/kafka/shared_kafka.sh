#!/bin/bash

set -e

# Login to Confluent Cloud if not logged in already
kafka_login() {
  LOGIN_RESULT=$(confluent kafka cluster list 2>&1 >/dev/null || true)
  if [[ $LOGIN_RESULT == *"not logged in"* || $LOGIN_RESULT == *"you must log in"* ]]; then
    confluent login 
  fi
}

# Select kafka environment
__kafka_env_select() {
  echo "The following environments are available:"
  confluent environment list
  while true; do
    echo
    read -p "Enter the id of the kafka environment: " KAFKA_ENV_ID
    KAFKA_ENV_SET=$(confluent environment use "$KAFKA_ENV_ID")
    echo "$KAFKA_ENV_SET"
    if [[ $KAFKA_ENV_SET == *"Using environment"* ]]; then
      break
    fi
  done
}

# Confirm kafka environment
kafka_env_confirm() {
  while true; do
    CURRENT_ENV_JSON=$(confluent environment list -o json | jq -r '.[] | select(.is_current)')
    CURRENT_ENV_ID=$(echo $CURRENT_ENV_JSON | jq -r '.id')
    CURRENT_ENV_NAME=$(echo $CURRENT_ENV_JSON | jq -r '.name')
    echo
    read -p "You are using the Kafka environment '$CURRENT_ENV_NAME' ($CURRENT_ENV_ID). Is this correct? (y/n) " KAFKA_ENV_CONF
    case $KAFKA_ENV_CONF in
      [Yy]* ) break;;
      [Nn]* ) __kafka_env_select;;
      * ) echo "Please answer (y)es or (n)o.";;
    esac
  done
}

# Select kafka cluster
__kafka_cluster_select() {
    echo "The following clusters are available:"
    confluent kafka cluster list
    read -p "Enter the id of the kafka cluster: " KAFKA_CLUSTER_ID
    confluent kafka cluster use "$KAFKA_CLUSTER_ID"
}

# Confirm kafka cluster
kafka_cluster_confirm() {
  while true; do
    CURRECT_CLUSTER_JSON=$(confluent kafka cluster list -o json | jq -r '.[] | select(.is_current)')
    CURRENT_CLUSTER_ID=$(echo $CURRECT_CLUSTER_JSON | jq -r '.id')
    CURRENT_CLUSTER_NAME=$(echo $CURRECT_CLUSTER_JSON | jq -r '.name')
    echo
    read -p "You are using the Kafka cluster '$CURRENT_CLUSTER_NAME' ($CURRENT_CLUSTER_ID). Is this correct? (y/n) " KAFKA_CLUSTER_CONF
    case $KAFKA_CLUSTER_CONF in
      [Yy]* ) break;;
      [Nn]* ) __kafka_cluster_select;;
      * ) echo "Please answer (y)es or (n)o.";;
    esac
  done
}

# Get kafka cluster id
kafka_cluster_id_get() {
  CLUSTER_ID=$(confluent kafka cluster list -o json | jq -r '.[] | select(.is_current) | .id')
  echo "$CLUSTER_ID"
}

# Get kafka cluster endpoint URL.
kafka_cluster_endpoint_url_get() {
  CLUSTER_ID=$(kafka_cluster_id_get)
  ENDPOINT_URL=$(confluent kafka cluster describe "$CLUSTER_ID" -o json| jq -r '.endpoint')
  echo "$ENDPOINT_URL"
}

# Creates a kafka service-account with the given name and description
create_service_account() {
  NAME=${1}
  DESCRIPTION=${2}

  echo "Creating service account: ${NAME} (${DESCRIPTION}) ..."
  RESPONSE=$(confluent iam service-account create "${NAME}" \
    --description "${DESCRIPTION}")
  echo "${RESPONSE}"

  ACCOUNT_ID=$(echo "${RESPONSE}" \
    | grep '[|] ID' \
    | cut -d '|' -f3 \
    | sed 's/[ ]*//g')
  echo "Using service account ${ACCOUNT_ID}"
}

# Finds the kafka service-account with the given name
find_service_account_id() {
  NAME=${1}
  ACCOUNT_ID=$(confluent iam service-account list -o json | jq -r --arg NAME "$NAME" '.[] | select(.name==$NAME) | .id')

  if [ -z "${ACCOUNT_ID}" ]
  then
    echo "Could not find a service account named ${NAME}."
    exit 1
  else
    echo "Found service account ${ACCOUNT_ID}"
  fi
}

# Deletes a kafka service-account with the given account id
delete_service_account() {
  ACCOUNT_ID=${1}
  echo "Deleting service account: ${ACCOUNT_ID} ..."
  confluent iam service-account delete ${ACCOUNT_ID}
}

# Creates an api-key with secret for the given service-account id, cluster id and description
create_api_key() {
  ACCOUNT_ID=${1}
  CLUSTER_ID=${2}
  DESCRIPTION=${3}

  echo "Creating API key for service account: ${ACCOUNT_ID} ..."
  RESPONSE=$(confluent api-key create \
    --service-account "${ACCOUNT_ID}" \
    --resource "${CLUSTER_ID}" \
    --description "${DESCRIPTION}" )
  echo "${RESPONSE}"

  API_KEY=$(echo "${RESPONSE}" \
    | tail -n4 \
    | grep 'API Key' \
    | cut -d '|' -f3 \
    | sed 's/[ ]*//g')
  API_SECRET=$(echo "${RESPONSE}" \
    | tail -n4 \
    | grep 'Secret' \
    | cut -d '|' -f3 \
    | sed 's/[ ]*//g')
  echo "Using API Key: ${API_KEY}"
  echo "Using API Secret: ${API_SECRET}"
}

# Finds the api-key with the given service-account id
find_api_key() {
  ACCOUNT_ID=${1}
  API_KEY=$(confluent api-key list -o json | jq -r --arg ACCOUNT_ID "$ACCOUNT_ID" '.[] | select(.owner_id==$ACCOUNT_ID) | .key')
}

# Deletes the given api-key
delete_api_key() {
  API_KEY=${1}
  echo "Deleting API key: ${API_KEY} ..."
  confluent api-key delete ${API_KEY}
}

# Select whether ACLs should be created or deleted
select_acl_operation() {
  while true; do
    echo
    read -p "Do you wish to (c)reate or (d)elete ACLs? (c/d): " ANSWER
    case $ANSWER in
      "c" ) ACL_OPERATION="create"; break;;
      "d" ) ACL_OPERATION="delete"; break;;
      * ) echo "Please enter 'c' or 'd'.";;
    esac
  done
}

# Login and confirm / select kafka environment / cluster
kafka_connect() {
  kafka_login
  kafka_env_confirm
  kafka_cluster_confirm
  CLUSTER_ID=$(kafka_cluster_id_get)
}

# Creates or overwrites the configuration properties file to be used by the kafka command line tools.
create_kafka_config_properties_file() {
  FILE_NAME=${1}
  API_KEY=${2}
  API_SECRET=${3}

  # Delete the file if already exists
  [ -e "$FILE_NAME" ] && rm "$FILE_NAME"

  cat > "$FILE_NAME" <<- EndOfMessage
request.timeout.ms=32000
retry.backoff.ms=500
ssl.endpoint.identification.algorithm=https
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${API_KEY}" password="${API_SECRET}";
security.protocol=SASL_SSL
EndOfMessage
}

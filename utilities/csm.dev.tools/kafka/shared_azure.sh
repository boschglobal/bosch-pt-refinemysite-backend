#!/bin/bash

set -e

# Login to azure if not logged in already
azure_login() {
  if [[ $(az account list 2>&1 >/dev/null) == *"az login"* ]]; then
    az login &>/dev/null
  fi
}

# Select azure subscription
azure_subscription_select() {
  echo
  echo "Available azure subscriptions:"
  az account list --query "[].name" --output tsv
  read -p 'Choose subscription: ' SUBSCRIPTION
  az account set --subscription "${SUBSCRIPTION}"
  SUBSCRIPTION_ID=$(az account show --subscription "${SUBSCRIPTION}" --query "@.id" --output tsv)
}

# Select key vault, provide question as function parameter, e.g. 'Choose environment-specific key vault: '.
# Selection is stored in the environment variable VAULT_NAME
azure_keyvault_select() {
  echo
  echo "Available azure key vaults:"
  az resource list --query "[?type == 'Microsoft.KeyVault/vaults'].name"  --output tsv
  read -p "${1}" VAULT_NAME
}

# Get the resource group name of the key vault with the name stored in the environment variable VAULT_NAME.
azure_keyvault_resource_group_get() {
  RESOURCE_GROUP=$(az resource list \
  --query "[?type == 'Microsoft.KeyVault/vaults' && name == '${VAULT_NAME}'].resourceGroup" \
  --output tsv)
  echo "$RESOURCE_GROUP"
}

# Reads a secret value stored for the given azure key vault and secret name
read_secret () {
  VAULT_NAME=${1}
  SECRET_NAME=${2}
  SECRET_VALUE=$(az keyvault secret show \
    --vault-name "${VAULT_NAME}" \
    --name "${SECRET_NAME}" \
    --query "value" \
    --output tsv)
  echo "${SECRET_VALUE}"
}

# Stores a secret in the given azure key vault
store_secret_in_keyvault () {
  VAULT_NAME=${1}
  SECRET_NAME=${2}
  SECRET_VALUE=${3}

  echo "Storing secret in Key Vault ${VAULT_NAME}:"
  az keyvault secret set \
    --vault-name "${VAULT_NAME}" \
    --name "${SECRET_NAME}" \
    --value "${SECRET_VALUE}"
}

store_secret_confirm_override () {
  VAULT_NAME=${1}
  SECRET_NAME=${2}
  SECRET_VALUE=${3}

  EXISTING_SECRET_VALUE=$(read_secret "${VAULT_NAME}" "${SECRET_NAME}" 2> /dev/null)
  if [ -z "${EXISTING_SECRET_VALUE}" ]; then
      store_secret_in_keyvault ${VAULT_NAME} ${SECRET_NAME} ${SECRET_VALUE}
  elif [ "${EXISTING_SECRET_VALUE}" != "${SECRET_VALUE}" ]; then
      echo -e "\nFound existing secret: \"${SECRET_NAME}\": \"${EXISTING_SECRET_VALUE}\""
      confirm "Override with value \"${SECRET_VALUE}\"? (y/n) "
      store_secret_in_keyvault ${VAULT_NAME} ${SECRET_NAME} ${SECRET_VALUE}
  else
      echo "Skipping unchanged value for ${SECRET_NAME}." 
  fi
}

# Deletes (and purges) a secret in the given azure key vault
delete_secret_in_key_vault() {
  VAULT_NAME=${1}
  SECRET_NAME=${2}

  echo "Deleting secret ${SECRET_NAME} from Key Vault: ${VAULT_NAME} ..."
  az keyvault secret delete --vault-name "${VAULT_NAME}" --name "${SECRET_NAME}"

  # Purge secret so that the secret name can be easily reused without having to restore its old values
  purge_secret_in_key_vault "${VAULT_NAME}" "${SECRET_NAME}"
}

# Purges a deleted secret in the given azure key vault.
# The secret must be deleted already. Otherwise this function will fail.
purge_secret_in_key_vault() {
  VAULT_NAME=${1}
  SECRET_NAME=${2}

  # wait for the secret to be actually deleted
  while [[ $(az keyvault secret show-deleted --vault-name "${VAULT_NAME}" --name "${SECRET_NAME}" 2>&1 >/dev/null) == *"Deleted Secret not found"* ]]; do
    echo "Secret is being deleted. Waiting for completion..."
    sleep 2
  done
  echo "Secret successfully found in deleted state."

  echo "Purging secret ${SECRET_NAME} from Key Vault: ${VAULT_NAME} ..."
  az keyvault secret purge --vault-name "${VAULT_NAME}" --name "${SECRET_NAME}"
  echo "Secret purged successfully."
}

# Login to azure and select subscription
azure_keyvault_connect() {
  QUESTION=${1}
  azure_login
  azure_subscription_select
  azure_keyvault_select "${QUESTION}"
  RESOURCE_GROUP=$(azure_keyvault_resource_group_get)

  echo
  echo "Secrets will be stored in / read from Key Vault ${VAULT_NAME} (Resource Group: ${RESOURCE_GROUP})"
  echo "Make sure that you are authorized for that Key Vault (Access Policies). Otherwise this script will fail!"
}

init_env_key_vault() {
  ENVIRONMENT=$1
  VAULT_NAME="ptcsm${ENVIRONMENT}envakskv"
  RESOURCE_GROUP=$(azure_keyvault_resource_group_get)
  echo "Using env key vault: ${VAULT_NAME} (${RESOURCE_GROUP})"
}

init_kafka_key_vault() {
  ENVIRONMENT=$1
  VAULT_NAME="ptcsm${ENVIRONMENT}kafkaakskv"
  RESOURCE_GROUP=$(azure_keyvault_resource_group_get)
  echo "Using kafka key vault: ${VAULT_NAME} (${RESOURCE_GROUP})"
}

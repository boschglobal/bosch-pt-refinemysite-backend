#!/bin/bash

# Creates Terraform configuration files.
# Load secrets from key vaults and writes them to the configuration.

set -e

TENANT_ID="REPLACE_ME"
DEV_SUBSCRIPTION_ID="REPLACE_ME"
QA_SUBSCRIPTION_ID="REPLACE_ME"
PROD_SUBSCRIPTION_ID="REPLACE_ME"
SANDBOX_SUBSCRIPTION_ID="REPLACE_ME"

USAGE="Usage: ${0} -s <sandbox|dev|qa|prod>"
SCRIPT_DIR=$(dirname ${0})

while getopts ":s:" OPT; do
    case ${OPT} in
    s)
        SUBSCRIPTION="${OPTARG}"
        ;;
    ?)
        echo "Invalid option: ${OPTARG}"
        echo "${USAGE}" 1>&2
        exit 1
        ;;
    esac
done

if [[ -z ${SUBSCRIPTION} ]]; then
    echo "Failed to extract subscription."
    echo "${USAGE}" 1>&2
    exit 1
fi

set_config() {
    local SUBSCRIPTION=${1}
    case ${SUBSCRIPTION} in
    "dev")
        MONGODB_ATLAS_PROJECT=PT_RefinemySite_DEV_211972
        SP_NAME=Azure-SP-REPLACE_ME-Dev-AzureDevOps
        SUBSCRIPTION_ID=${DEV_SUBSCRIPTION_ID}
        ;;
    "prod")
        MONGODB_ATLAS_PROJECT=PT_RefinemySite_PROD_211972
        SP_NAME=Azure-SP-REPLACE_ME-Prod-AzureDevOps
        SUBSCRIPTION_ID=${PROD_SUBSCRIPTION_ID}
        ;;
    "qa")
        MONGODB_ATLAS_PROJECT=PT_RefinemySite_DEV_211972
        SP_NAME=Azure-SP-REPLACE_ME-QA-AzureDevOps
        SUBSCRIPTION_ID=${QA_SUBSCRIPTION_ID}
        ;;
    "sandbox")
        MONGODB_ATLAS_PROJECT=PT_RefinemySite_DEV_211972
        SP_NAME=Azure-SP-REPLACE_ME-Sandbox-AzureDevOps
        SUBSCRIPTION_ID=${SANDBOX_SUBSCRIPTION_ID}
        ;;
    *)
        echo "Unknown subscription: ${1}"
        echo "${USAGE}" 1>&2
        exit 1
        ;;
    esac
}

print_config() {
    cat <<EOF
Config = {
    MONGODB_ATLAS_PROJECT = ${MONGODB_ATLAS_PROJECT}
    SP_NAME               = ${SP_NAME}
    SUBSCRIPTION          = ${SUBSCRIPTION}
    SUBSCRIPTION_ID       = ${SUBSCRIPTION_ID}
    TENANT_ID             = ${TENANT_ID}
}
EOF
}

# Login to azure if not logged in already
azure_login() {
    if [[ $(az account list 2>&1 >/dev/null) == *"az login"* ]]; then
        az login &>/dev/null
    fi
}

# Confirm (Yy) question or exit (Nn), question has to be provided as function parameter
confirm_or_exit() {
    QUESTION="${1}"
    while true; do
        read -p "$QUESTION" YES_OR_NO
        case $YES_OR_NO in
        [Yy]*) break ;;
        [Nn]*) exit 0 ;;
        *) echo "Please answer (y)es or (n)o." ;;
        esac
    done
}

# Reads a secret value stored for the given azure key vault and secret name
read_secret() {
    local VAULT_NAME=${1}
    local SECRET_NAME=${2}
    local SECRET_VALUE=$(az keyvault secret show \
        --vault-name "${VAULT_NAME}" \
        --name "${SECRET_NAME}" \
        --query "value" \
        --output tsv)
    echo "${SECRET_VALUE}"
}

set_config ${SUBSCRIPTION}
print_config

azure_login
az account set --subscription ${SUBSCRIPTION_ID}

# Backend configuration
STORAGE_ACCOUNT_RESOURCE_GROUP="pt-csm-${SUBSCRIPTION}-tf-state"
STORAGE_ACCOUNT_NAME="ptcsm${SUBSCRIPTION}tfstate"
STORAGE_ACCOUNT_CONTAINER_NAME="pt-csm-${SUBSCRIPTION}-tf-state"
STORAGE_ACCOUNT_KEY1=$(az storage account keys list -g ${STORAGE_ACCOUNT_RESOURCE_GROUP} -n ${STORAGE_ACCOUNT_NAME} --query "[0].[value]" -o tsv)

echo "Creating backend configuration ${SCRIPT_DIR}/backend-${SUBSCRIPTION}.tfvars ..."
cat <<EOF >${SCRIPT_DIR}/backend-${SUBSCRIPTION}.tfvars
access_key           = "${STORAGE_ACCOUNT_KEY1}"
container_name       = "${STORAGE_ACCOUNT_CONTAINER_NAME}"
storage_account_name = "${STORAGE_ACCOUNT_NAME}"
EOF
echo "Backend configuration created."

# Service principal configuration
KEY_VAULT_NAME="ptcsm${SUBSCRIPTION}pipeline"
CLIENT_ID=$(read_secret ${KEY_VAULT_NAME} client-id)
CLIENT_SECRET=$(read_secret ${KEY_VAULT_NAME} client-secret)

echo "Creating service principal configuration ${SCRIPT_DIR}/sp-${SUBSCRIPTION}.tfvars ..."
cat <<EOF >${SCRIPT_DIR}/sp-${SUBSCRIPTION}.tfvars
# Azurerm (service principal ${SP_NAME})
client_id       = "${CLIENT_ID}"
client_secret   = "${CLIENT_SECRET}"
subscription_id = "${SUBSCRIPTION_ID}"
tenant_id       = "${TENANT_ID}"
EOF
echo "Service principal configuration created."

# MongoDB Atlas configuration
MONGODBATLAS_PRIVATE_KEY=$(read_secret ${KEY_VAULT_NAME} mongodbatlas-private-key)
MONGODBATLAS_PUBLIC_KEY=$(read_secret ${KEY_VAULT_NAME} mongodbatlas-public-key)

echo "Creating MongoDB Atlas configuration ${SCRIPT_DIR}/mongodbatlas-${SUBSCRIPTION}.tfvars ..."
cat <<EOF >${SCRIPT_DIR}/mongodbatlas-${SUBSCRIPTION}.tfvars
# MongoDB Atlas API Key (Project ${MONGODB_ATLAS_PROJECT})
mongodbatlas_private_key = "${MONGODBATLAS_PRIVATE_KEY}"
mongodbatlas_public_key  = "${MONGODBATLAS_PUBLIC_KEY}"
EOF
echo "MongoDB Atlas configuration created."

# Azure DevOps configuration
AZUREDEVOPS_PAT=$(read_secret ${KEY_VAULT_NAME} azure-devops-token-terraform)

echo "Creating Azure DevOps configuration ${SCRIPT_DIR}/azuredevops.tfvars ..."
cat <<EOF >${SCRIPT_DIR}/azuredevops.tfvars
# Azure DevOps PAT (Organisation https://dev.azure.com/pt-iot)
azuredevops_personal_access_token = "${AZUREDEVOPS_PAT}"
EOF
echo "Azure DevOps configuration created."

# Confluent Cloud configuration
CONFLUENT_CLOUD_API_KEY=$(read_secret ${KEY_VAULT_NAME} confluent-cloud-api-key)
CONFLUENT_CLOUD_API_SECRET=$(read_secret ${KEY_VAULT_NAME} confluent-cloud-api-secret)

echo "Creating Confluent Cloud configuration ${SCRIPT_DIR}/confluentcloud.tfvars ..."
cat <<EOF >${SCRIPT_DIR}/confluentcloud.tfvars
# Confluent Cloud API Key (tf_runner)
confluent_cloud_api_key    = "${CONFLUENT_CLOUD_API_KEY}"
confluent_cloud_api_secret = "${CONFLUENT_CLOUD_API_SECRET}"
EOF
echo "Confluent Cloud configuration created."

# Prompt for moving files
TF_CONFIG_DIR="${HOME}/.tf-config"
echo
confirm_or_exit "Move created configuration files and override files in \"${TF_CONFIG_DIR}\"? (y/n)"

# Move files
echo "Moving configuration files ${SCRIPT_DIR}/*.tfvars to ${TF_CONFIG_DIR} ..."
mkdir -p ${TF_CONFIG_DIR}
mv ${SCRIPT_DIR}/*.tfvars ${TF_CONFIG_DIR}
echo "Configuration files moved."

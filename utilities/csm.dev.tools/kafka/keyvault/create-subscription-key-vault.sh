#!/bin/bash

# Allows to create a subscription-specfic key vault. Guides the user
# through the creation process; no command-line arguments are required.
#
# We don't use Terraform for this, because we only set up *environments*
# using Terraform, not entire subscriptions.

set -e

PROD_SUBSCRIPTION_ID="REPLACE_ME"

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"

# Prints a list of all key vaults in the current subcription
print_key_vault_names () {
  VAULT_NAMES=$(az resource list \
    | grep '"id":.*/providers/Microsoft.KeyVault/.*"' \
    | awk -F\" '{print $4}' \
    | cut -d '/' -f9)
  echo "Available Azure KeyVault names:"
  echo "${VAULT_NAMES}"
}

# Creates a key vault and the corresponding resource group
create_key_vault () {
  VAULT_NAME=${1}
  VAULT_RESOURCE_GROUP=${2}
  VAULT_SKU=${3}
  LOCATION="West Europe"

  echo "Creating resource group ${VAULT_RESOURCE_GROUP}..."
  az group create \
    --name "${VAULT_RESOURCE_GROUP}" \
    --location "${LOCATION}"

  echo "Creating key vault ${VAULT_NAME}..."
  az keyvault create \
    --name "${VAULT_NAME}" \
    --resource-group "${VAULT_RESOURCE_GROUP}" \
    --sku "${VAULT_SKU}" \
    --location "${LOCATION}"
}

print_azure_devops_applications () {
  echo "Fetching list of Azure DevOps applications. This may take a while..."
  az ad app list --all --output table \
    --query "[?contains(@.displayName, 'Azure-SP-PT-ENS-RefineMySite') && contains(@.displayName, 'AzureDevops') ].{\"application Name\": displayName, \"Application Id\": appId}"
}

# Grants key vault access to a given Azure application
grant_key_vault_access_to_application () {
  VAULT_NAME=${1}
  APPLICATION_ID=${2}

  az keyvault set-policy --name "${VAULT_NAME}" \
    --spn "${APPLICATION_ID}" \
    --secret-permissions get list
}

# Login to azure and select subscription
azure_login
azure_subscription_select

echo
print_key_vault_names

VAULT_NAME_SUGGESTION="ptcsm${STAGE}subakskv"
RESOURCE_GROUP_SUGGESTION="pt-csm-${STAGE}-sub-aks-kv"
VAULT_SKU_SUGGESTION="standard"

confirm "Create new Key Vault ${VAULT_NAME_SUGGESTION} (resource group ${RESOURCE_GROUP_SUGGESTION})? (y/n) "

# Use Key Vault with premium SKU for prod subscription
if [[ ${SUBSCRIPTION_ID} == ${PROD_SUBSCRIPTION_ID} ]]; then
  VAULT_SKU_SUGGESTION="premium"
fi

create_key_vault "${VAULT_NAME_SUGGESTION}" "${RESOURCE_GROUP_SUGGESTION}" "${VAULT_SKU_SUGGESTION}"

# Ask user for the Application that shall be granted access to the key vault
echo
echo "Preparing to grant key vault access permissions to the subscription's Azure DevOps application..."
print_azure_devops_applications
read -p 'Grant key vault access to application with id: ' APPLICATION_ID
grant_key_vault_access_to_application "${VAULT_NAME_SUGGESTION}" "${APPLICATION_ID}"

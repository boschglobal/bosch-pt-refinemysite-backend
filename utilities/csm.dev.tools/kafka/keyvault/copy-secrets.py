#!/usr/bin/env python3
import sys

sys.path.insert(0, "../utils")

from re import match
from sys import exit

from azure_keyvault import AzureKeyVaultClient
from azure_subscription import AzureSubscriptionClient
from azure.core.exceptions import ResourceNotFoundError
from common import CommonUtils

# Get subscription and key-vaults
subscription_name = AzureSubscriptionClient.select_subscription()
env_vault = AzureKeyVaultClient.select_env_key_vault()
kafka_vault = AzureKeyVaultClient.get_kafka_vault_by_env_vault_name(env_vault.name)

# Determine the resource group of the key-vaults
resource_group_pattern = '.*/resourceGroups/(.*)/providers/.*'
env_vault_rg = match(resource_group_pattern, env_vault.id).group(1)
kafka_vault_rg = match(resource_group_pattern, kafka_vault.id).group(1)

# Ask the user to confirm before continue
print('\nAccessing key-vaults: {} (resource group: {}), {} (resource group: {})'.format(
    env_vault.name, env_vault_rg, kafka_vault.name, kafka_vault_rg))

print('Make sure that you are authorized for the key vaults (Access Policies). Otherwise this script will fail!')
CommonUtils.confirm('\nProceed with color check and calculation of changeset (y/n)?')

# Do a quick-check whether secrets of correct color will be copied
print('\nDo a quick kafka color check...')
kafka_color = AzureKeyVaultClient.get_secret(env_vault, 'kafka-color').value

env_project_api_key = AzureKeyVaultClient.get_secret(env_vault, 'csm-cloud-project-kafka-broker-api-key').value
blue_project_api_key = None
green_project_api_key = None
try:
    # Get broker api key blue
    print('Checking csm-cloud-project-kafka-broker-api-key-blue...')
    blue_project_api_key = AzureKeyVaultClient.get_secret(
        kafka_vault, 'csm-cloud-project-kafka-broker-api-key-blue').value
except ResourceNotFoundError as e:
    print('Secret csm-cloud-project-kafka-broker-api-key-blue does not exist.')
try:
    # Get broker api key green
    print('Checking csm-cloud-project-kafka-broker-api-key-green...')
    green_project_api_key = AzureKeyVaultClient.get_secret(
        kafka_vault, 'csm-cloud-project-kafka-broker-api-key-green').value
except ResourceNotFoundError as e:
    print('Secret csm-cloud-project-kafka-broker-api-key-green does not exist.')

env_schema_registry_api_key = AzureKeyVaultClient.get_secret(env_vault, 'kafka-schemaregistry-api-key').value
blue_schema_registry_api_key = AzureKeyVaultClient.get_secret(kafka_vault, 'kafka-schemaregistry-api-key-blue').value
green_schema_registry_api_key = AzureKeyVaultClient.get_secret(kafka_vault, 'kafka-schemaregistry-api-key-green').value

# Check that a valid kafka color is configured in the key vault
print('\nCurrent kafka color:', kafka_color)
if kafka_color not in ['blue', 'green']:
    print('Invalid kafka color found in env key vault. Valid options are: [blue, green]')
    exit(1)

# Check correct broker color
if kafka_color == 'blue' and env_project_api_key != blue_project_api_key:
    CommonUtils.confirm('\nPotential broker api change detected. If you are in the middle of an api key rotation, continue (y). Otherwise stop execution (n).')
if kafka_color == 'green' and env_project_api_key != green_project_api_key:
    CommonUtils.confirm('\nPotential broker api change detected. If you are in the middle of an api key rotation, continue (y). Otherwise stop execution (n).')

# Check correct schema registry color
if kafka_color == 'blue' and env_schema_registry_api_key != blue_schema_registry_api_key:
    print('\nPotential schema registry api change detected. Abort execution.')
    exit(1)
if kafka_color == 'green' and env_schema_registry_api_key != green_schema_registry_api_key:
    print('\nPotential schema registry api change detected. Abort execution.')
    exit(1)

print('Color check - done\n')

# Get and print secrets
print('Calculate changeset...')

print('- Load secrets from env key vault (this may take some time)')
env_kv_secrets = AzureKeyVaultClient.get_secrets(env_vault, None)
env_secrets = dict()
for secret_property in env_kv_secrets:
    secret_name = AzureKeyVaultClient.get_secret_name(secret_property)
    env_secrets[secret_name] = AzureKeyVaultClient.get_secret(env_vault, secret_name).value

print('- Load secrets from kafka key vault (this may take some time)')
kafka_kv_secrets = AzureKeyVaultClient.get_secrets(kafka_vault, kafka_color)
kafkakv_secrets = dict()
for secret_property in kafka_kv_secrets:
    secret_name = AzureKeyVaultClient.get_secret_name(secret_property)
    secret_value = AzureKeyVaultClient.get_secret(kafka_vault, secret_name).value
    kafkakv_secrets[secret_name] = (secret_value, secret_property)

if len(kafka_kv_secrets) == 0:
    print('No secrets in kafka key vault found')
    exit(1)

# Find added / changed secrets
new_secrets = list()
changed_secrets = list()
obsolete_secrets = list()

for secret_name, value in kafkakv_secrets.items():
    secret_value = value[0]
    secret_property = value[1]
    secret_name_without_color = secret_name[: -(len(kafka_color) + 1)]
    env_secret = env_secrets.get(secret_name_without_color)
    if env_secret is None:
        new_secrets.append(secret_property)
    elif env_secret != secret_value:
        changed_secrets.append(secret_property)

# Find obsolete secrets which are present in env key vault, but not in kafka key vault
for secret_name in env_secrets.keys():
    secret_name_with_color = secret_name + '-' + kafka_color
    kafka_secret = kafkakv_secrets.get(secret_name_with_color)
    if kafka_secret is None and ("kafka-broker" in secret_name or "kafka-schemaregistry" in secret_name):
        obsolete_secrets.append(secret_name)

vault_uri = kafka_vault.properties.vault_uri
secrets_to_copy = list()

# Print added / changed secrets to copy
if len(new_secrets) > 0:
    print('\nNew secrets to copy: ')
    [print(secret.id[len(vault_uri) + len('secrets/'):]) for secret in new_secrets]
    secrets_to_copy.extend(new_secrets)
if len(changed_secrets) > 0:
    print('\nChanged secrets to copy: ')
    [print(secret.id[len(vault_uri) + len('secrets/'):]) for secret in changed_secrets]
    secrets_to_copy.extend(changed_secrets)
if len(obsolete_secrets) > 0:
    print('\nObsolete secrets that can be removed from env key vault: ')
    [print(secret) for secret in obsolete_secrets]

if len(secrets_to_copy) == 0:
    print('\nNo secrets to copy found')
    exit(1)

# Ask the user to confirm before continue
CommonUtils.confirm('\nDo you want to copy above mentioned secrets from kafka to env key vault (y/n)?')

# Copy secrets into the target key vault
for secret_property in secrets_to_copy:
    AzureKeyVaultClient.copy_secret(
        env_vault, secret_property, kafka_color, True, '')

print('Done')

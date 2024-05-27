#!/usr/bin/env python3
import sys
sys.path.insert(0,"../utils")

from re import match

from azure_subscription import AzureSubscriptionClient
from azure_keyvault import AzureKeyVaultClient
from common import CommonUtils


# Select subscription, and key-vaults
subscription_name = AzureSubscriptionClient.select_subscription()
env_vault = AzureKeyVaultClient.select_env_vault()
kafka_vault = AzureKeyVaultClient.get_kafka_vault_by_env_vault_name(
    env_vault.name)

# Determine the resource group of the key-vaults
resource_group_pattern = '.*/resourceGroups/(.*)/providers/.*'
env_vault_rg = match(resource_group_pattern, env_vault.id).group(1)
kafka_vault_rg = match(resource_group_pattern, kafka_vault.id).group(1)

# Ask the user to confirm before continue
print('Accessing key-vaults: {} (resource group: {}), {} (resource group: {})'.format(
    env_vault.name, env_vault_rg, kafka_vault.name, kafka_vault_rg))
print('Make sure that you are authorized for the key vaults (Access Policies). Otherwise this script will fail!')
CommonUtils.confirm()

# Get the blue/green broker URLs from kafka key-vault and the URL from the env key-vault
env_broker_urls = AzureKeyVaultClient.get_secret(
    env_vault, 'kafka-broker-urls')
blue_broker_urls = AzureKeyVaultClient.get_secret(
    kafka_vault, 'kafka-broker-urls-blue')
green_broker_urls = AzureKeyVaultClient.get_secret(
    kafka_vault, 'kafka-broker-urls-green')

# Check which URL is in the env key-vault
if green_broker_urls.value == blue_broker_urls.value:
    print('Blue and green clusters are the same - color cannot be determined')
elif env_broker_urls.value == blue_broker_urls.value:
    print('The blue cluster is used')
elif env_broker_urls.value == green_broker_urls.value:
    print('The green cluster is used')
else:
    print('Neither blue nor green cluster is used')
    print('Blue:', blue_broker_urls)
    print('Green:', green_broker_urls)
    print('Env:', env_broker_urls)

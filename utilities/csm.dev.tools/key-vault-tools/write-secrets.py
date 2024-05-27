#!/usr/bin/env python3

import json
import subprocess
import sys
import textwrap

try:
    from azure.mgmt.keyvault import KeyVaultManagementClient
except ImportError:
    sys.exit(
        'Please install missing dependency via: pip install azure-mgmt-keyvault or pip3 install azure-mgmt-keyvault')

try:
    from azure.mgmt.subscription import SubscriptionClient
except ImportError:
    sys.exit(
        'Please install missing dependency via: pip install azure-mgmt-subscription or pip3 install azure-mgmt-subscription')

try:
    from azure.keyvault import KeyVaultClient, KeyVaultId
except ImportError:
    sys.exit('Please install missing dependency via: pip install azure-keyvault or pip3 install azure-keyvault')

try:
    from azure.common.client_factory import get_client_from_cli_profile
except ImportError:
    sys.exit('Please install missing dependency via: pip install azure-cli-core or pip3 install azure-cli-core')

from azure.keyvault.v7_0.models.secret_attributes import SecretAttributes


class ProcessUtils:
    @staticmethod
    def run(operation, executable_path='', working_directory=''):
        if working_directory == '':
            working_directory = sys.path[0]

        command = executable_path + operation
        p = subprocess.Popen(command, cwd=working_directory, shell=True,
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)

        out, err = p.communicate()
        output = out.decode('utf-8').strip(' \t\n\r')
        error = err.decode('utf-8').strip(' \t\n\r')
        return output, error

    @staticmethod
    def print(output, error):
        wrapper = textwrap.TextWrapper(initial_indent='   ', subsequent_indent='   ')
        if output != '':
            print(wrapper.fill(output))
        if error != '':
            print(wrapper.fill(error))


# Print available azure subscriptions
print('Select azure subscription:')
subscription_client = get_client_from_cli_profile(SubscriptionClient)
subscriptions_list = list(subscription_client.subscriptions.list())
for sub in subscriptions_list:
    print(sub.display_name)
subscription_name = input('Enter subscription name: ')
matched_subscriptions = [subscription.display_name for subscription in subscriptions_list
                         if subscription.display_name == subscription_name]
if len(matched_subscriptions) != 1 or subscription_name != matched_subscriptions[0]:
    print('Invalid subscription name provided')
    sys.exit(1)

# Set selected azure subscription
output, error = ProcessUtils.run('az account set --subscription ' + subscription_name)
ProcessUtils.print(output, error)

# Print available key-vaults
print('Select key-vault:')
kv_mgmt_client = get_client_from_cli_profile(KeyVaultManagementClient)
vaults = list(kv_mgmt_client.vaults.list_by_subscription())
for vault in vaults:
    print(vault.name)

# Select key vault
key_vault_name = input('Enter key-vault name: ')
matched_vaults = [vault for vault in vaults if vault.name == key_vault_name]
if len(matched_vaults) != 1 or key_vault_name != matched_vaults[0].name:
    print('Invalid key vault name provided')
    sys.exit(1)
vault = matched_vaults[0]

print('Make sure that you are authorized for that key vault (Access Policies). Otherwise this script will fail!')

# Get secrets from key-vault
kv_client = get_client_from_cli_profile(KeyVaultClient)
vault_uri = vault.properties.vault_uri
vault_secrets = list(kv_client.get_secrets(vault_uri))

# Read json file with secrets
file_name = input('Enter secret file-name: ')
with open(file_name, 'r') as json_file:
    data = json.load(json_file)

    # Save secrets in key vault
    for secret in data['secrets']:
        print('Save secret:', secret['name'])
        if len([vault_secret for vault_secret in vault_secrets
                if vault_secret.id[len(vault_uri) + len('secrets/'):] == secret['name']]) > 0:
            skip = False
            while True:
                selection = input('Secret already exists. Skip/Delete/Exit (s/d/e)? ')
                if 's' == selection:
                    skip = True
                    break
                elif 'd' == selection:
                    break
                elif 'e' == selection:
                    sys.exit(1)
            if skip:
                continue
            else:
                kv_client.delete_secret(vault_uri, secret['name'])

        secret_attributes = SecretAttributes(enabled=secret['attributes']['enabled'],
                                             not_before=secret['attributes']['not_before'],
                                             expires=secret['attributes']['expires'])

        kv_client.set_secret(vault_uri, secret['name'], secret['value'], secret['tags'],
                                             secret['content_type'], secret_attributes)

print("Done")

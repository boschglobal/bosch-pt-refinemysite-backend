from re import compile
from sys import exit

from azure.identity import AzureCliCredential
from azure.keyvault.secrets import SecretClient
from azure.mgmt.keyvault import KeyVaultManagementClient

from azure_subscription import AzureSubscriptionClient


class AzureKeyVaultClient:
    @staticmethod
    def select_source_vault():
        """Select the source key-vault"""

        return AzureKeyVaultClient.select_key_vault('Select source key-vault:')

    @staticmethod
    def select_target_vault():
        """Select the target key-vault"""

        return AzureKeyVaultClient.select_key_vault('Select target key-vault:')

    @staticmethod
    def select_env_vault():
        """Select the environment key-vault"""

        return AzureKeyVaultClient.select_key_vault('Select env key-vault:')
    
    @staticmethod
    def select_env_key_vault():
        """Select env key-vault of the currently selected azure subscription"""

        print('\nSelect azure key-vault:')

        # Print available key-vaults
        subscription_id = AzureSubscriptionClient.get_subscription_id()
        kv_mgmt_client = KeyVaultManagementClient(AzureCliCredential(), subscription_id)
        vaults = list(kv_mgmt_client.vaults.list_by_subscription())
        for vault in [v for v in vaults if 'envakskv' in v.name]:
            print(vault.name)

        # Select key vault
        key_vault_name = input('Enter env key-vault name: ')
        matched_vaults = [
            vault for vault in vaults if vault.name == key_vault_name]
        if len(matched_vaults) != 1 or key_vault_name != matched_vaults[0].name:
            print('Invalid key vault name provided')
            exit(1)
        vault = matched_vaults[0]

        return vault

    @staticmethod
    def select_key_vault(message):
        """Select a key-vault of the currently selected azure subscription"""

        print(message)

        # Print available key-vaults
        subscription_id = AzureSubscriptionClient.get_subscription_id()
        kv_mgmt_client = KeyVaultManagementClient(AzureCliCredential(), subscription_id)
        vaults = list(kv_mgmt_client.vaults.list_by_subscription())
        for vault in vaults:
            print(vault.name)

        # Select key vault
        key_vault_name = input('Enter key-vault name: ')
        matched_vaults = [
            vault for vault in vaults if vault.name == key_vault_name]
        if len(matched_vaults) != 1 or key_vault_name != matched_vaults[0].name:
            print('Invalid key vault name provided')
            exit(1)
        vault = matched_vaults[0]

        return vault

    @staticmethod
    def get_kafka_vault_by_env_vault_name(env_vault_name):
        """Select the kafka vault by the given env vault name"""

        kafka_vault_name = env_vault_name[:-len('envakskv')] + 'kafkaakskv'

        subscription_id = AzureSubscriptionClient.get_subscription_id()
        kv_mgmt_client = KeyVaultManagementClient(AzureCliCredential(), subscription_id)
        vaults = list(kv_mgmt_client.vaults.list_by_subscription())
        return AzureKeyVaultClient.assert_vault_exists(vaults, kafka_vault_name)

    @staticmethod
    def assert_vault_exists(vaults, key_vault_name):
        """Asserts that a key-vault with a given name exists"""

        matched_vaults = [vault for vault in vaults if vault.name == key_vault_name]
        if len(matched_vaults) != 1 or key_vault_name != matched_vaults[0].name:
            print('Invalid key vault name provided')
            exit(1)
        return matched_vaults[0]

    @staticmethod
    def get_secrets(vault, source_filter):
        """Get secrets from key-vault"""

        vault_uri = vault.properties.vault_uri
        kv_client = SecretClient(vault_uri, AzureCliCredential())
        secrets = list(kv_client.list_properties_of_secrets())
        if(source_filter in ['blue', 'green']):
            secrets = [
                secret for secret in secrets if secret.id.endswith(source_filter)]
        elif source_filter in ['kafka']:
            pattern = compile('.*csm-.*-api-key|.*csm-.*-api-secret')
            secrets = [
                secret for secret in secrets if pattern.match(secret.id)]

        return secrets

    @staticmethod
    def get_secret(vault, secret_name):
        """Get a secret from key-vault"""

        vault_uri = vault.properties.vault_uri
        kv_client = SecretClient(vault_uri, AzureCliCredential())
        return kv_client.get_secret(secret_name, '')

    @staticmethod
    def get_secret_name(secret_proprety):
        """Returns a secret name from SecretProperty"""

        return secret_proprety.id[secret_proprety.id.index("secrets/") + len('secrets/'):]

    @staticmethod
    def copy_secret(vault, secret, source_filter, source_remove_suffix, target_suffix):
        """Add or create new version of a secret in key-vault"""

        target_vault_uri = vault.properties.vault_uri
        target_kv_client = SecretClient(target_vault_uri, AzureCliCredential())

        # Get the original secret name
        secret_name = secret.id[secret.id.index("secrets/") + len('secrets/'):]

        # Generate name of the secret in the target kev-vault
        new_secret_name = secret_name
        if source_remove_suffix and source_filter in secret_name:
            new_secret_name = secret_name[: -(len(source_filter) + 1)]
        if len(target_suffix) > 0:
            new_secret_name = new_secret_name + "-" + target_suffix
        print("Copy secret:", secret_name, "->", new_secret_name)

        source_vault_uri = secret.id[:secret.id.index("secrets/")]
        source_kv_client = SecretClient(source_vault_uri, AzureCliCredential())
        secret_value = source_kv_client.get_secret(secret_name, '')

        target_kv_client.set_secret(new_secret_name, secret_value.value)

    @staticmethod
    def delete_secret(vault, secret_name):
        """Deletes a secret in the given key-vault"""

        vault_uri = vault.properties.vault_uri
        kv_client = SecretClient(vault_uri, AzureCliCredential())

        # Delete the secret
        print('Delete:', secret_name)
        kv_client.begin_delete_secret(secret_name).wait()
        kv_client.purge_deleted_secret(secret_name)

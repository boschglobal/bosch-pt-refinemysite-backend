from sys import exit

from process import ProcessUtils

from azure.identity import AzureCliCredential
from azure.mgmt.subscription import SubscriptionClient


class AzureSubscriptionClient:
    @staticmethod
    def select_subscription():
        """Select the azure subscription by reading authentication information via azure-cli (az) login"""

        # Print available azure subscriptions
        print('Select azure subscription:')
        subscription_client = SubscriptionClient(AzureCliCredential())
        subscriptions_list = list(subscription_client.subscriptions.list())
        for sub in subscriptions_list:
            print(sub.display_name)
        subscription_name = input('Enter subscription name: ')
        matched_subscriptions = [subscription.display_name for subscription in subscriptions_list
                                 if subscription.display_name == subscription_name]
        if len(matched_subscriptions) != 1 or subscription_name != matched_subscriptions[0]:
            print('Invalid subscription name provided')
            exit(1)

        # Set selected azure subscription
        output, error = ProcessUtils.run(
            'az account set --subscription ' + subscription_name)
        ProcessUtils.print(output, error)

        return subscription_name

    @staticmethod
    def get_subscription_id():
        """Get the currently selected subscription from azure-cli (az)"""

        output, error = ProcessUtils.run("az account list | jq '.[] | select(.isDefault==true) | .id' --raw-output")
        if(len(error) > 0):
            print("Couldn't detekt azure subscription")
            exit(1)
        return output

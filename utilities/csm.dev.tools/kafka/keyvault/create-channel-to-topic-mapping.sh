#!/bin/bash

# Creates the initial Kafka topic configuration for an environment by
# creating a KeyVault secret for each channel. The secret value is the
# desired kafka topic for that channel.

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"

init_environment_from_args $@ 
init_env_key_vault $ENVIRONMENT

echo
confirm 'Continue creating the initial Kafka channel-to-topic mapping? (y/n) '

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-bim-model" \
    "csm.${ENV}.bim.model"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-company" \
    "csm.${ENV}.companymanagement.company"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-craft" \
    "csm.${ENV}.referencedata.craft"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-event" \
    "csm.${ENV}.event"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-event-mobile-command" \
    "csm.${ENV}.event.mobile.command"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-job-command" \
    "csm.${ENV}.job.command"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-job-event" \
    "csm.${ENV}.job.event"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-project" \
    "csm.${ENV}.projectmanagement.project"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-project-delete" \
    "csm.${ENV}.projectmanagement.project.delete"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-project-invitation" \
    "csm.${ENV}.projectmanagement.project.invitation"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-user" \
    "csm.${ENV}.usermanagement.user"

store_secret_confirm_override "${VAULT_NAME}" "kafka-topic-consents" \
    "csm.${ENV}.usermanagement.consents"

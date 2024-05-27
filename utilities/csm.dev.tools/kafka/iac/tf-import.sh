#!/bin/bash

set -e

source "$(dirname ${BASH_SOURCE[0]})/../shared.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_azure.sh"
source "$(dirname ${BASH_SOURCE[0]})/../shared_kafka.sh"

USAGE=$"
-e:env[The environment for which the plan will be executed.]{sandbox1-4|dev|review|test1|prod1}
-s:subscription[The subscription for which the plan will be executed.]{sandbox|dev|qa|prod}
"

while getopts ":e:s:" OPT; do
    case ${OPT} in
    e)
        ENV="${OPTARG}"
        ;;
    s)
        SUBSCRIPTION="${OPTARG}"
        ;;
    ?)
        echo "Invalid option: ${OPTARG}"
        echo "Usage: ${USAGE}"
        exit 1
        ;;
    esac
done

if [[ -z ${ENV} ]]; then
    echo "Failed to extract environment."
    exit 2
fi

if [[ -z ${SUBSCRIPTION} ]]; then
    echo "Failed to extract subscription."
    exit 2
fi

CONFLUENT_CLOUD_API_KEY=~/.tf-config/confluentcloud.tfvars
if [[ ! -f ${CONFLUENT_CLOUD_API_KEY} ]]; then
    echo "Confluent Cloud API Key ${CONFLUENT_CLOUD_API_KEY} does not exist."
    exit 2
fi

SP_CONFIG=~/.tf-config/sp-${SUBSCRIPTION}.tfvars
if [[ ! -f ${SP_CONFIG} ]]; then
    echo "SP configuration ${SP_CONFIG} does not exist."
    exit 2
fi

ENV_CONFIG=config/${ENV}.tfvars
if [[ ! -f ${ENV_CONFIG} ]]; then
    echo "Environment configuration ${ENV_CONFIG} does not exist."
    exit 2
fi

case ${ENV} in
"dev")
    ENV_NAME=csm-dev
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=dev
    ENV_NAME_WITHOUT_COLOR_SHORT=dev
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"test1")
    ENV_NAME=csm-test1
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=test1
    ENV_NAME_WITHOUT_COLOR_SHORT=t1
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"review-blue")
    ENV_NAME=csm-review-blue
    ENV_COLOR=blue
    ENV_COLOR_SHORT=blu
    ENV_NAME_WITHOUT_COLOR=review
    ENV_NAME_WITHOUT_COLOR_SHORT=rev
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"review-green")
    ENV_NAME=csm-review-green
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=review
    ENV_NAME_WITHOUT_COLOR_SHORT=rev
    # Enable services
    IS_BACKUP_CLUSTER=true
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=false
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"prod-blue")
    ENV_NAME=csm-production-blue
    ENV_COLOR=blue
    ENV_COLOR_SHORT=blu
    ENV_NAME_WITHOUT_COLOR=prod
    ENV_NAME_WITHOUT_COLOR_SHORT=prod
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=true
    RESET_ENABLED=false
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"prod-green")
    ENV_NAME=csm-production-green
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=prod
    ENV_NAME_WITHOUT_COLOR_SHORT=prod
    # Enable services
    IS_BACKUP_CLUSTER=true
    KAFKA_BACKUP_ENABLED=true
    RESET_ENABLED=false
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"sandbox1")
    ENV_NAME=csm-sandbox1
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=sandbox1
    ENV_NAME_WITHOUT_COLOR_SHORT=s1
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"sandbox2")
    ENV_NAME=csm-sandbox2
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=sandbox2
    ENV_NAME_WITHOUT_COLOR_SHORT=s2
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"sandbox3")
    ENV_NAME=csm-sandbox3
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=sandbox3
    ENV_NAME_WITHOUT_COLOR_SHORT=s3
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
"sandbox4")
    ENV_NAME=csm-sandbox4
    ENV_COLOR=green
    ENV_COLOR_SHORT=gre
    ENV_NAME_WITHOUT_COLOR=sandbox4
    ENV_NAME_WITHOUT_COLOR_SHORT=s4
    # Enable services
    IS_BACKUP_CLUSTER=false
    KAFKA_BACKUP_ENABLED=false
    RESET_ENABLED=true
    # Active API Key (primary or secondary)
    ACTIVE_API_KEY=secondary
    ;;
*)
    echo "Unknown environment name entered."
    ;;
esac

echo "Env and Cluster name: ${ENV_NAME}"
echo "Env color: ${ENV_COLOR}"
echo "Env color short: ${ENV_COLOR_SHORT}"
echo "Env name without color: ${ENV_NAME_WITHOUT_COLOR}"
echo "Env name short without color: ${ENV_NAME_WITHOUT_COLOR_SHORT}"

RED="\033[0;31m"
REDB="\033[1;31m"
GREEN="\033[0;32m"
GREENB="\033[1;32m"
YELLOW="\033[0;33m"
YELLOWB="\033[1;33m"
BLUE="\033[0;34m"
BLUEB="\033[1;34m"
CLEAR="\033[0m"

is_tf_resource_in_state() {
    # Terraform resource pattern is "type.name" or "type.name[0]"
    local TF_RESOURCE=${1}
    local TF_RESOURCE_TYPE=$(echo ${TF_RESOURCE} | cut -d '.' -f1)
    local TF_RESOURCE_NAME=$(echo ${TF_RESOURCE} | cut -d '.' -f2 | cut -d '[' -f1)
    local TF_RESOURCE_FOUND=$(cat ${TF_STATE} | jq -r ".resources[] | select(.name==\"${TF_RESOURCE_NAME}\") | select(.type==\"${TF_RESOURCE_TYPE}\")")
    if [[ -z ${TF_RESOURCE_FOUND} ]]; then
        echo false
    else
        echo true
    fi
}

find_service_account_id_by_name() {
    local SERVICE_NAME=${1}
    local SA_ID=$(confluent iam service-account list -o json | jq -r --arg NAME "csm-${ENV_NAME_WITHOUT_COLOR_SHORT}-${SERVICE_NAME}-${ENV_COLOR_SHORT}" '.[] | select(.name==$NAME) | .id')
    if [ -z "${SA_ID}" ]; then
        echo -e "${REDB}Could not find a service account named ${NAME}.${CLEAR}"
        exit 1
    else
        echo "${SA_ID}"
    fi
}

read_secret_id() {
    local VAULT_NAME=${1}
    local SECRET_NAME=${2}
    SECRET_ID=$(az keyvault secret show \
        --vault-name "${VAULT_NAME}" \
        --name "${SECRET_NAME}" \
        --query "id" \
        --output tsv)
    echo "${SECRET_ID}"
}

import_kafka_resource() {
    local TF_RESOURCE=${1}
    local KAFKA_RESOURCE=${2}
    if $(is_tf_resource_in_state ${TF_RESOURCE}); then
        echo -e "${YELLOWB}Skip import ${TF_RESOURCE}. Resource is already in state.${CLEAR}"
    else
        terraform import -var-file=${CONFLUENT_CLOUD_API_KEY} -var-file=${SP_CONFIG} -var-file=${ENV_CONFIG} ${TF_RESOURCE} ${KAFKA_RESOURCE}
    fi
}

import_key_vault_secret() {
    local TF_RESOURCE=${1}
    local VAULT_NAME=${2}
    local SECRET_NAME=${3}
    if $(is_tf_resource_in_state ${TF_RESOURCE}); then
        echo -e "${YELLOWB}Skip import ${TF_RESOURCE}. Resource is already in state.${CLEAR}"
    else
        local KV_SECRET_ID=$(read_secret_id "${VAULT_NAME}" "${SECRET_NAME}")
        terraform import -var-file=${CONFLUENT_CLOUD_API_KEY} -var-file=${SP_CONFIG} -var-file=${ENV_CONFIG} ${TF_RESOURCE} ${KV_SECRET_ID}
    fi
}

# Pull terraform state
TF_STATE=${ENV}-confluent.tfstate
terraform state pull >${TF_STATE}

kafka_login

# Import environment
ENV_ID=$(confluent environment list -o json | jq -r --arg NAME "${ENV_NAME}" '.[] | select(.name==$NAME) | .id')
echo
echo "Set environment to: ${ENV_ID}"
confluent environment use ${ENV_ID}
confluent environment list
echo
import_kafka_resource confluent_environment.this ${ENV_ID}

# Import cluster
CLUSTER_ID=$(confluent kafka cluster list -o json | jq -r --arg NAME "${ENV_NAME}" '.[] | select(.name==$NAME) | .id')
echo
echo "Set cluster to: ${CLUSTER_ID}"
confluent kafka cluster use ${CLUSTER_ID}
confluent kafka cluster list
echo
import_kafka_resource confluent_kafka_cluster.this ${ENV_ID}/${CLUSTER_ID}

# Run terraform apply to create app manager
if $(is_tf_resource_in_state confluent_api_key.terraform); then
    echo -e "${YELLOWB}Skip apply confluent_api_key.terraform. Resource is already in state.${CLEAR}"
else
    terraform apply -var-file=${CONFLUENT_CLOUD_API_KEY} -var-file=${SP_CONFIG} -var-file=${ENV_CONFIG} -target confluent_api_key.terraform
    # Pull terraform state because of new resource
    terraform state pull >${TF_STATE}
fi

azure_login
echo "Set azure subscription to: ${SUBSCRIPTION}"
az account set --subscription PT-BDO-OF-RefineMySite-${SUBSCRIPTION}
az account show
echo

init_kafka_key_vault ${ENV_NAME_WITHOUT_COLOR}

# Import kafka-broker-url from kafka key vault
import_key_vault_secret azurerm_key_vault_secret.kafka_broker_urls "${VAULT_NAME}" "kafka-broker-urls-${ENV_COLOR}"

# Get the api key, secret and rest endpoint from terraform state
export IMPORT_KAFKA_API_KEY=$(cat ${TF_STATE} | jq -r ".resources[] | select(.name==\"terraform\") | select(.type==\"confluent_api_key\") | .instances[] | .attributes.id")
export IMPORT_KAFKA_API_SECRET=$(cat ${TF_STATE} | jq -r ".resources[] | select(.name==\"terraform\") | select(.type==\"confluent_api_key\") | .instances[] | .attributes.secret")
export IMPORT_KAFKA_REST_ENDPOINT=$(cat ${TF_STATE} | jq -r ".resources[] | select(.name==\"this\") | select(.type==\"confluent_kafka_cluster\") | .instances[] | .attributes.rest_endpoint")

init_env_key_vault ${ENV_NAME_WITHOUT_COLOR}

# Import topics
TOPIC_BIM_MODEL=$(read_secret "${VAULT_NAME}" "kafka-topic-bim-model")
TOPIC_COMPANY=$(read_secret "${VAULT_NAME}" "kafka-topic-company")
TOPIC_USER_CONSENTS=$(read_secret "${VAULT_NAME}" "kafka-topic-consents")
TOPIC_USER_CRAFT=$(read_secret "${VAULT_NAME}" "kafka-topic-craft")
TOPIC_EVENT=$(read_secret "${VAULT_NAME}" "kafka-topic-event")
TOPIC_EVENT_MOBILE_COMMAND=$(read_secret "${VAULT_NAME}" "kafka-topic-event-mobile-command")
TOPIC_FEATURETOGGLE=$(read_secret "${VAULT_NAME}" "kafka-topic-featuretoggle")
TOPIC_IMG_SCLE=$(read_secret "${VAULT_NAME}" "kafka-topic-image-scale")
TOPIC_JOB_COMMAND=$(read_secret "${VAULT_NAME}" "kafka-topic-job-command")
TOPIC_JOB_EVENT=$(read_secret "${VAULT_NAME}" "kafka-topic-job-event")
TOPIC_USER_PAT=$(read_secret "${VAULT_NAME}" "kafka-topic-pat")
TOPIC_PROJECT=$(read_secret "${VAULT_NAME}" "kafka-topic-project")
TOPIC_PROJECT_DELETE=$(read_secret "${VAULT_NAME}" "kafka-topic-project-delete")
TOPIC_PROJECT_INVITATION=$(read_secret "${VAULT_NAME}" "kafka-topic-project-invitation")
TOPIC_STRG_EVNT=$(read_secret "${VAULT_NAME}" "kafka-topic-storage-event")
TOPIC_USER=$(read_secret "${VAULT_NAME}" "kafka-topic-user")
echo
echo "Using the following topics:"
echo "BIM model topic: ${TOPIC_BIM_MODEL}"
echo "Company topic: ${TOPIC_COMPANY}"
echo "Consents topic: ${TOPIC_USER_CONSENTS}"
echo "Craft topic: ${TOPIC_USER_CRAFT}"
echo "Event topic: ${TOPIC_EVENT}"
echo "Event Mobile Command topic: ${TOPIC_EVENT_MOBILE_COMMAND}"
echo "Feature topic: ${TOPIC_FEATURETOGGLE}"
echo "Image Scale topic: ${TOPIC_IMG_SCLE}"
echo "Job command topic: ${TOPIC_JOB_COMMAND}"
echo "Job event topic: ${TOPIC_JOB_EVENT}"
echo "Project topic: ${TOPIC_PROJECT}"
echo "Project-delete topic: ${TOPIC_PROJECT_DELETE}"
echo "Project-invitation topic: ${TOPIC_PROJECT_INVITATION}"
echo "Storage-Event topic: ${TOPIC_STRG_EVNT}"
echo "User topic: ${TOPIC_USER}"
echo "User PAT topic: ${TOPIC_USER_PAT}"
echo

import_kafka_resource confluent_kafka_topic.user ${CLUSTER_ID}/${TOPIC_USER}
import_kafka_resource confluent_kafka_topic.user_consents ${CLUSTER_ID}/${TOPIC_USER_CONSENTS}
import_kafka_resource confluent_kafka_topic.user_craft ${CLUSTER_ID}/${TOPIC_USER_CRAFT}
import_kafka_resource confluent_kafka_topic.user_pat ${CLUSTER_ID}/${TOPIC_USER_PAT}
import_kafka_resource confluent_kafka_topic.event ${CLUSTER_ID}/${TOPIC_EVENT}
import_kafka_resource confluent_kafka_topic.event_mobile_command ${CLUSTER_ID}/${TOPIC_EVENT_MOBILE_COMMAND}
import_kafka_resource confluent_kafka_topic.job_command ${CLUSTER_ID}/${TOPIC_JOB_COMMAND}
import_kafka_resource confluent_kafka_topic.job_event ${CLUSTER_ID}/${TOPIC_JOB_EVENT}
import_kafka_resource confluent_kafka_topic.project ${CLUSTER_ID}/${TOPIC_PROJECT}
import_kafka_resource confluent_kafka_topic.project_delete ${CLUSTER_ID}/${TOPIC_PROJECT_DELETE}
import_kafka_resource confluent_kafka_topic.project_invitation ${CLUSTER_ID}/${TOPIC_PROJECT_INVITATION}
import_kafka_resource confluent_kafka_topic.company ${CLUSTER_ID}/${TOPIC_COMPANY}
import_kafka_resource confluent_kafka_topic.bim_model ${CLUSTER_ID}/${TOPIC_BIM_MODEL}
import_kafka_resource confluent_kafka_topic.storage_event ${CLUSTER_ID}/${TOPIC_STRG_EVNT}
import_kafka_resource confluent_kafka_topic.featuretoggle ${CLUSTER_ID}/${TOPIC_FEATURETOGGLE}
import_kafka_resource confluent_kafka_topic.image_scale ${CLUSTER_ID}/${TOPIC_IMG_SCLE}

# Import service accounts
if ${IS_BACKUP_CLUSTER}; then
    echo -e "${YELLOWB}Skip import of service accounts for backup cluster.${CLEAR}"
else
    SA_USER=$(find_service_account_id_by_name user)
    import_kafka_resource confluent_service_account.user ${SA_USER}
    SA_EVENT=$(find_service_account_id_by_name event)
    import_kafka_resource confluent_service_account.event ${SA_EVENT}
    SA_PRO_STA=$(find_service_account_id_by_name pro-sta)
    import_kafka_resource confluent_service_account.project_statistics ${SA_PRO_STA}
    SA_JOB=$(find_service_account_id_by_name job)
    import_kafka_resource confluent_service_account.job ${SA_JOB}
    SA_PROJECT=$(find_service_account_id_by_name project)
    import_kafka_resource confluent_service_account.project ${SA_PROJECT}
    SA_COMPANY=$(find_service_account_id_by_name company)
    import_kafka_resource confluent_service_account.company ${SA_COMPANY}
    SA_BIM_MODEL=$(find_service_account_id_by_name bim-model)
    import_kafka_resource confluent_service_account.bim_model ${SA_BIM_MODEL}
    SA_EVENT_MOBILE=$(find_service_account_id_by_name event-mobile)
    import_kafka_resource confluent_service_account.event_mobile ${SA_EVENT_MOBILE}
    SA_STRG_EVNT=$(find_service_account_id_by_name strg-evnt)
    import_kafka_resource confluent_service_account.storage_event ${SA_STRG_EVNT}
    SA_FEATURETOGGLE=$(find_service_account_id_by_name toggle)
    import_kafka_resource confluent_service_account.featuretoggle ${SA_FEATURETOGGLE}
    SA_IMG_SCLE=$(find_service_account_id_by_name img-scle)
    import_kafka_resource confluent_service_account.image_scale ${SA_IMG_SCLE}
    SA_USER_KAF_CON=$(find_service_account_id_by_name use-kaf-con)
    import_kafka_resource confluent_service_account.user_kafka_connector ${SA_USER_KAF_CON}
    SA_COMPANY_KAF_CON=$(find_service_account_id_by_name com-kaf-con)
    import_kafka_resource confluent_service_account.company_kafka_connector ${SA_COMPANY_KAF_CON}
    SA_PROJECT_KAF_CON=$(find_service_account_id_by_name pro-kaf-con)
    import_kafka_resource confluent_service_account.project_kafka_connector ${SA_PROJECT_KAF_CON}
    SA_FEATURE_KAF_CON=$(find_service_account_id_by_name toggle-kaf-con)
    import_kafka_resource confluent_service_account.feature_kafka_connector ${SA_FEATURE_KAF_CON}
    SA_PRO_NOT=$(find_service_account_id_by_name pro-not)
    import_kafka_resource confluent_service_account.project_notifications ${SA_PRO_NOT}
    SA_PRO_NEW=$(find_service_account_id_by_name pro-new)
    import_kafka_resource confluent_service_account.project_news ${SA_PRO_NEW}
    SA_PRO_ACY=$(find_service_account_id_by_name pro-acy)
    import_kafka_resource confluent_service_account.project_activity ${SA_PRO_ACY}
    SA_PRO_API_TS=$(find_service_account_id_by_name pro-api-ts)
    import_kafka_resource confluent_service_account.project_api_timeseries ${SA_PRO_API_TS}
    if ${RESET_ENABLED}; then
        echo "Import Reset Service Account"
        SA_RESET=$(find_service_account_id_by_name reset)
        import_kafka_resource "confluent_service_account.reset[0]" ${SA_RESET}
    fi
    SA_BAM_IMP=$(find_service_account_id_by_name met-imp)
    import_kafka_resource confluent_service_account.bam_importer ${SA_BAM_IMP}
    SA_KAFKA_LAG_EXPORTER=$(find_service_account_id_by_name met-kep)
    import_kafka_resource confluent_service_account.kafka_lag_exporter ${SA_KAFKA_LAG_EXPORTER}
    SA_KAFKA_MIGRATOR=$(find_service_account_id_by_name migrator)
    import_kafka_resource "confluent_service_account.kafka_migrator" ${SA_KAFKA_MIGRATOR}
fi

SA_READONLY=$(find_service_account_id_by_name readonly)
import_kafka_resource confluent_service_account.readonly ${SA_READONLY}
SA_DEL_CGP=$(find_service_account_id_by_name del-cgp)
import_kafka_resource confluent_service_account.delete_consumer_groups ${SA_DEL_CGP}
if ${KAFKA_BACKUP_ENABLED}; then
    echo "Import Backup Service Account"
    SA_KAFKA_BACKUP=$(find_service_account_id_by_name backup)
    import_kafka_resource "confluent_service_account.kafka_backup[0]" ${SA_KAFKA_BACKUP}
fi

confluent iam service-account list | grep "csm-${ENV_NAME_WITHOUT_COLOR_SHORT}"
echo

init_kafka_key_vault ${ENV_NAME_WITHOUT_COLOR}

# Import api keys
if ${IS_BACKUP_CLUSTER}; then
    echo -e "${YELLOWB}Skip import of api keys for backup cluster.${CLEAR}"
else
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-user-kafka-broker-api-secret-${ENV_COLOR}")
    AK_USER=$(read_secret "${VAULT_NAME}" "csm-cloud-user-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.user_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_USER}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-event-kafka-broker-api-secret-${ENV_COLOR}")
    AK_EVENT=$(read_secret "${VAULT_NAME}" "csm-cloud-event-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.event_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_EVENT}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-statistics-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PRO_STA=$(read_secret "${VAULT_NAME}" "csm-cloud-project-statistics-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_statistics_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PRO_STA}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-job-kafka-broker-api-secret-${ENV_COLOR}")
    AK_JOB=$(read_secret "${VAULT_NAME}" "csm-cloud-job-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.job_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_JOB}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PROJECT=$(read_secret "${VAULT_NAME}" "csm-cloud-project-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PROJECT}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-company-kafka-broker-api-secret-${ENV_COLOR}")
    AK_COMPANY=$(read_secret "${VAULT_NAME}" "csm-cloud-company-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.company_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_COMPANY}
    # export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-bim-model-kafka-broker-api-secret-${ENV_COLOR}")
    # AK_BIM_MODEL=$(read_secret "${VAULT_NAME}" "csm-cloud-bim-model-kafka-broker-api-key-${ENV_COLOR}")
    # import_kafka_resource "confluent_api_key.bim_model_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_BIM_MODEL}
    # export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-event-mobile-kafka-broker-api-secret-${ENV_COLOR}")
    # AK_EVENT_MOBILE=$(read_secret "${VAULT_NAME}" "csm-cloud-event-mobile-kafka-broker-api-key-${ENV_COLOR}")
    # import_kafka_resource "confluent_api_key.event_mobile_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_EVENT_MOBILE}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-storage-event-kafka-broker-api-secret-${ENV_COLOR}")
    AK_STRG_EVNT=$(read_secret "${VAULT_NAME}" "csm-cloud-storage-event-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.storage_event_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_STRG_EVNT}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-featuretoggle-kafka-broker-api-secret-${ENV_COLOR}")
    AK_FEATURETOGGLE=$(read_secret "${VAULT_NAME}" "csm-cloud-featuretoggle-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.featuretoggle_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_FEATURETOGGLE}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-image-scale-kafka-broker-api-secret-${ENV_COLOR}")
    AK_IMG_SCLE=$(read_secret "${VAULT_NAME}" "csm-cloud-image-scale-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.image_scale_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_IMG_SCLE}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-user-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}")
    AK_USER_KAF_CON=$(read_secret "${VAULT_NAME}" "csm-cloud-user-kafka-connector-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.user_kafka_connector_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_USER_KAF_CON}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-company-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}")
    AK_COMPANY_KAF_CON=$(read_secret "${VAULT_NAME}" "csm-cloud-company-kafka-connector-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.company_kafka_connector_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_COMPANY_KAF_CON}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PROJECT_KAF_CON=$(read_secret "${VAULT_NAME}" "csm-cloud-project-kafka-connector-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_kafka_connector_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PROJECT_KAF_CON}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-feature-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}")
    AK_FEATURE_KAF_CON=$(read_secret "${VAULT_NAME}" "csm-cloud-feature-kafka-connector-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.feature_kafka_connector_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_FEATURE_KAF_CON}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-notifications-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PRO_NOT=$(read_secret "${VAULT_NAME}" "csm-cloud-project-notifications-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_notifications_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PRO_NOT}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-news-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PRO_NEW=$(read_secret "${VAULT_NAME}" "csm-cloud-project-news-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_news_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PRO_NEW}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-activity-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PRO_ACY=$(read_secret "${VAULT_NAME}" "csm-cloud-project-activity-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_activity_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PRO_ACY}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-project-api-timeseries-kafka-broker-api-secret-${ENV_COLOR}")
    AK_PRO_API_TS=$(read_secret "${VAULT_NAME}" "csm-cloud-project-api-timeseries-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.project_api_timeseries_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_PRO_API_TS}
    if ${RESET_ENABLED}; then
        echo "Import Reset Api Key"
        export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-reset-kafka-broker-api-secret-${ENV_COLOR}")
        AK_RESET=$(read_secret "${VAULT_NAME}" "csm-cloud-reset-kafka-broker-api-key-${ENV_COLOR}")
        import_kafka_resource "confluent_api_key.reset_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_RESET}
    fi
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-monitoring-metrics-influxdb-importer-kafka-broker-api-secret-${ENV_COLOR}")
    AK_BAM_IMP=$(read_secret "${VAULT_NAME}" "csm-monitoring-metrics-influxdb-importer-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.bam_importer_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_BAM_IMP}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-secret-${ENV_COLOR}")
    AK_KAFKA_LAG_EXPORTER=$(read_secret "${VAULT_NAME}" "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.kafka_lag_exporter_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_KAFKA_LAG_EXPORTER}
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-kafka-migrator-kafka-broker-api-secret-${ENV_COLOR}")
    AK_KAFKA_MIGRATOR=$(read_secret "${VAULT_NAME}" "csm-cloud-kafka-migrator-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.kafka_migrator_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_KAFKA_MIGRATOR}
fi

export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-read-only-kafka-broker-api-secret-${ENV_COLOR}")
AK_READONLY=$(read_secret "${VAULT_NAME}" "csm-read-only-kafka-broker-api-key-${ENV_COLOR}")
import_kafka_resource "confluent_api_key.readonly_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_READONLY}
export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-dev-del-cgp-kafka-broker-api-secret-${ENV_COLOR}")
AK_DEL_CGP=$(read_secret "${VAULT_NAME}" "csm-dev-del-cgp-kafka-broker-api-key-${ENV_COLOR}")
import_kafka_resource "confluent_api_key.delete_consumer_groups_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_DEL_CGP}
if ${KAFKA_BACKUP_ENABLED}; then
    echo "Import Backup Api Key"
    export API_KEY_SECRET=$(read_secret "${VAULT_NAME}" "csm-cloud-backup-kafka-broker-api-secret-${ENV_COLOR}")
    AK_KAFKA_BACKUP=$(read_secret "${VAULT_NAME}" "csm-cloud-backup-kafka-broker-api-key-${ENV_COLOR}")
    import_kafka_resource "confluent_api_key.kafka_backup_${ACTIVE_API_KEY}[0]" ${ENV_ID}/${AK_KAFKA_BACKUP}
fi

# Import ACLs
if ${IS_BACKUP_CLUSTER}; then
    echo -e "${YELLOWB}Skip import of ACLs for backup cluster.${CLEAR}"
else
    import_kafka_resource confluent_kafka_acl.user_group_read "${CLUSTER_ID}/GROUP#csm-um-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_USER}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_USER}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.event_group_read "${CLUSTER_ID}/GROUP#csm-event-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_EVENT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.event_topic_read_event "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT}#LITERAL#User:${SA_EVENT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_statistics_group_read "${CLUSTER_ID}/GROUP#csm-pm-statistics-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_PRO_STA}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_statistics_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_STA}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_group_read "${CLUSTER_ID}/GROUP#csm-cloud-job-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_JOB}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_JOB}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_topic_write_job_event "${CLUSTER_ID}/TOPIC#${TOPIC_JOB_EVENT}#LITERAL#User:${SA_JOB}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_topic_write_event "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT}#LITERAL#User:${SA_JOB}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cloud-job-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_JOB}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.job_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cloud-job-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_JOB}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_group_read "${CLUSTER_ID}/GROUP#csm-pm-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PROJECT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PROJECT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_topic_write_project_delete "${CLUSTER_ID}/TOPIC#${TOPIC_PROJECT_DELETE}#LITERAL#User:${SA_PROJECT}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_topic_write_job_command "${CLUSTER_ID}/TOPIC#${TOPIC_JOB_COMMAND}#LITERAL#User:${SA_PROJECT}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.company_group_read "${CLUSTER_ID}/GROUP#csm-cm-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_COMPANY}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.company_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_COMPANY}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_group_read "${CLUSTER_ID}/GROUP#csm-cloud-bim-model-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_BIM_MODEL}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_BIM_MODEL}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_topic_write_bim_model "${CLUSTER_ID}/TOPIC#${TOPIC_BIM_MODEL}#LITERAL#User:${SA_BIM_MODEL}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_topic_write_event "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT}#LITERAL#User:${SA_BIM_MODEL}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cloud-bim-model-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_BIM_MODEL}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.bim_model_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cloud-bim-model-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_BIM_MODEL}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.event_mobile_group_read "${CLUSTER_ID}/GROUP#csm-cloud-event-mobile-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_EVENT_MOBILE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.event_mobile_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_EVENT_MOBILE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.storage_event_topic_write_storage_event "${CLUSTER_ID}/TOPIC#${TOPIC_STRG_EVNT}#LITERAL#User:${SA_STRG_EVNT}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.featuretoggle_group_read "${CLUSTER_ID}/GROUP#csm-fm-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_FEATURETOGGLE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.featuretoggle_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_FEATURETOGGLE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.image_scale_group_read "${CLUSTER_ID}/GROUP#csm-im-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_IMG_SCLE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.image_scale_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_IMG_SCLE}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.image_scale_topic_write "${CLUSTER_ID}/TOPIC#${TOPIC_IMG_SCLE}#LITERAL#User:${SA_IMG_SCLE}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_topic_write_user "${CLUSTER_ID}/TOPIC#${TOPIC_USER}#LITERAL#User:${SA_USER_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_topic_write_user_consents "${CLUSTER_ID}/TOPIC#${TOPIC_USER_CONSENTS}#LITERAL#User:${SA_USER_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_topic_write_user_craft "${CLUSTER_ID}/TOPIC#${TOPIC_USER_CRAFT}#LITERAL#User:${SA_USER_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_topic_write_user_pat "${CLUSTER_ID}/TOPIC#${TOPIC_USER_PAT}#LITERAL#User:${SA_USER_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-um-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_USER_KAF_CON}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.user_kafka_connector_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-um-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_USER_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.company_kafka_connector_topic_write_company "${CLUSTER_ID}/TOPIC#${TOPIC_COMPANY}#LITERAL#User:${SA_COMPANY_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.company_kafka_connector_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_COMPANY_KAF_CON}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.company_kafka_connector_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-cm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_COMPANY_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_kafka_connector_topic_write_project "${CLUSTER_ID}/TOPIC#${TOPIC_PROJECT}#LITERAL#User:${SA_PROJECT_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_kafka_connector_topic_write_project_invitation "${CLUSTER_ID}/TOPIC#${TOPIC_PROJECT_INVITATION}#LITERAL#User:${SA_PROJECT_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_kafka_connector_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-pm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PROJECT_KAF_CON}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_kafka_connector_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-pm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PROJECT_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.feature_kafka_connector_topic_write_featuretoggle "${CLUSTER_ID}/TOPIC#${TOPIC_FEATURETOGGLE}#LITERAL#User:${SA_FEATURE_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.feature_kafka_connector_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-fm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_FEATURE_KAF_CON}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.feature_kafka_connector_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-fm-kafka-connector-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_FEATURE_KAF_CON}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_notifications_cluster_idempotent_write "${CLUSTER_ID}/CLUSTER#kafka-cluster#LITERAL#User:${SA_PRO_NOT}#*#IDEMPOTENT_WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_notifications_group_read "${CLUSTER_ID}/GROUP#csm-pm-notifications-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_PRO_NOT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_notifications_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_NOT}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_notifications_topic_write_event "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT}#LITERAL#User:${SA_PRO_NOT}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_notification_topic_write_event_mobile_command "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT_MOBILE_COMMAND}#LITERAL#User:${SA_PRO_NOT}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_news_cluster_idempotent_write "${CLUSTER_ID}/CLUSTER#kafka-cluster#LITERAL#User:${SA_PRO_NEW}#*#IDEMPOTENT_WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_news_group_read "${CLUSTER_ID}/GROUP#csm-pm-news-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_PRO_NEW}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_news_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_NEW}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_news_topic_write_event "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT}#LITERAL#User:${SA_PRO_NEW}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_news_topic_write_event_mobile_command "${CLUSTER_ID}/TOPIC#${TOPIC_EVENT_MOBILE_COMMAND}#LITERAL#User:${SA_PRO_NEW}#*#WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_activity_group_read "${CLUSTER_ID}/GROUP#csm-pm-activity-g2-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_PRO_ACY}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_activity_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_ACY}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_api_timeseries_group_read "${CLUSTER_ID}/GROUP#csm-pm-api-timeseries-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_API_TS}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.project_api_timeseries_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_PRO_API_TS}#*#READ#ALLOW"
    if ${RESET_ENABLED}; then
        echo "Import Reset ACL's"
        import_kafka_resource "confluent_kafka_acl.reset_topic_create[0]" "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_RESET}#*#CREATE#ALLOW"
        import_kafka_resource "confluent_kafka_acl.reset_topic_delete[0]" "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_RESET}#*#DELETE#ALLOW"
    fi
    import_kafka_resource confluent_kafka_acl.bam_importer_group_read "${CLUSTER_ID}/GROUP#csm-monitoring-metrics-influxdb-importer-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_BAM_IMP}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.bam_importer_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_BAM_IMP}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_lag_exporter_group_describe "${CLUSTER_ID}/GROUP#*#LITERAL#User:${SA_KAFKA_LAG_EXPORTER}#*#DESCRIBE#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_lag_exporter_topic_describe "${CLUSTER_ID}/TOPIC#*#LITERAL#User:${SA_KAFKA_LAG_EXPORTER}#*#DESCRIBE#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_migrator_cluster_idempotent_write "${CLUSTER_ID}/CLUSTER#kafka-cluster#LITERAL#User:${SA_KAFKA_MIGRATOR}#*#IDEMPOTENT_WRITE#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_migrator_group_read "${CLUSTER_ID}/GROUP#csm-migrator-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_KAFKA_MIGRATOR}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_migrator_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_MIGRATOR}#*#READ#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_migrator_transactional_create "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-migrator-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_MIGRATOR}#*#CREATE#ALLOW"
    import_kafka_resource confluent_kafka_acl.kafka_migrator_transactional_write "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-migrator-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_MIGRATOR}#*#WRITE#ALLOW"
fi

import_kafka_resource confluent_kafka_acl.readonly_group_read "${CLUSTER_ID}/GROUP#*#LITERAL#User:${SA_READONLY}#*#READ#ALLOW"
import_kafka_resource confluent_kafka_acl.readonly_topic_read "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_READONLY}#*#READ#ALLOW"
import_kafka_resource confluent_kafka_acl.delete_consumer_groups_group_delete "${CLUSTER_ID}/GROUP#*#LITERAL#User:${SA_DEL_CGP}#*#DELETE#ALLOW"
import_kafka_resource confluent_kafka_acl.delete_consumer_groups_group_describe "${CLUSTER_ID}/GROUP#*#LITERAL#User:${SA_DEL_CGP}#*#DESCRIBE#ALLOW"
if ${KAFKA_BACKUP_ENABLED}; then
    if ${IS_BACKUP_CLUSTER}; then
        echo "Import Backup ACL's with create and write permissions"
        import_kafka_resource "confluent_kafka_acl.kafka_backup_topic_write[0]" "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_BACKUP}#*#WRITE#ALLOW"
        import_kafka_resource "confluent_kafka_acl.kafka_backup_transactional_create[0]" "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-backup-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_BACKUP}#*#CREATE#ALLOW"
        import_kafka_resource "confluent_kafka_acl.kafka_backup_transactional_write[0]" "${CLUSTER_ID}/TRANSACTIONAL_ID#csm-backup-${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_BACKUP}#*#WRITE#ALLOW"
    else
        echo "Import Backup ACL's with read permissions"
        import_kafka_resource "confluent_kafka_acl.kafka_backup_group_read[0]" "${CLUSTER_ID}/GROUP#csm-backup-${ENV_NAME_WITHOUT_COLOR}#LITERAL#User:${SA_KAFKA_BACKUP}#*#READ#ALLOW"
        import_kafka_resource "confluent_kafka_acl.kafka_backup_topic_read[0]" "${CLUSTER_ID}/TOPIC#csm.${ENV_NAME_WITHOUT_COLOR}#PREFIXED#User:${SA_KAFKA_BACKUP}#*#READ#ALLOW"
    fi
fi

# Import Key Vault Secrets
if ${IS_BACKUP_CLUSTER}; then
    echo -e "${YELLOWB}Skip import of key vault secrets for backup cluster.${CLEAR}"
else
    import_key_vault_secret azurerm_key_vault_secret.user_api_key_key "${VAULT_NAME}" "csm-cloud-user-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.user_api_key_secret "${VAULT_NAME}" "csm-cloud-user-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.event_api_key_key "${VAULT_NAME}" "csm-cloud-event-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.event_api_key_secret "${VAULT_NAME}" "csm-cloud-event-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_statistics_api_key_key "${VAULT_NAME}" "csm-cloud-project-statistics-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_statistics_api_key_secret "${VAULT_NAME}" "csm-cloud-project-statistics-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.job_api_key_key "${VAULT_NAME}" "csm-cloud-job-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.job_api_key_secret "${VAULT_NAME}" "csm-cloud-job-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_api_key_key "${VAULT_NAME}" "csm-cloud-project-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_api_key_secret "${VAULT_NAME}" "csm-cloud-project-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.company_api_key_key "${VAULT_NAME}" "csm-cloud-company-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.company_api_key_secret "${VAULT_NAME}" "csm-cloud-company-kafka-broker-api-secret-${ENV_COLOR}"
    # import_key_vault_secret azurerm_key_vault_secret.bim_model_api_key_key "${VAULT_NAME}" "csm-cloud-bim-model-kafka-broker-api-key-${ENV_COLOR}"
    # import_key_vault_secret azurerm_key_vault_secret.bim_model_api_key_secret "${VAULT_NAME}" "csm-cloud-bim-model-kafka-broker-api-secret-${ENV_COLOR}"
    # import_key_vault_secret azurerm_key_vault_secret.event_mobile_api_key_key "${VAULT_NAME}" "csm-cloud-event-mobile-kafka-broker-api-key-${ENV_COLOR}"
    # import_key_vault_secret azurerm_key_vault_secret.event_mobile_api_key_secret "${VAULT_NAME}" "csm-cloud-event-mobile-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.storage_event_api_key_key "${VAULT_NAME}" "csm-cloud-storage-event-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.storage_event_api_key_secret "${VAULT_NAME}" "csm-cloud-storage-event-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.featuretoggle_api_key_key "${VAULT_NAME}" "csm-cloud-featuretoggle-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.featuretoggle_api_key_secret "${VAULT_NAME}" "csm-cloud-featuretoggle-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.image_scale_api_key_key "${VAULT_NAME}" "csm-cloud-image-scale-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.image_scale_api_key_secret "${VAULT_NAME}" "csm-cloud-image-scale-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.user_kafka_connector_api_key_key "${VAULT_NAME}" "csm-cloud-user-kafka-connector-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.user_kafka_connector_api_key_secret "${VAULT_NAME}" "csm-cloud-user-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.company_kafka_connector_api_key_key "${VAULT_NAME}" "csm-cloud-company-kafka-connector-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.company_kafka_connector_api_key_secret "${VAULT_NAME}" "csm-cloud-company-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_kafka_connector_api_key_key "${VAULT_NAME}" "csm-cloud-project-kafka-connector-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_kafka_connector_api_key_secret "${VAULT_NAME}" "csm-cloud-project-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.feature_kafka_connector_api_key_key "${VAULT_NAME}" "csm-cloud-feature-kafka-connector-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.feature_kafka_connector_api_key_secret "${VAULT_NAME}" "csm-cloud-feature-kafka-connector-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_notifications_api_key_key "${VAULT_NAME}" "csm-cloud-project-notifications-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_notifications_api_key_secret "${VAULT_NAME}" "csm-cloud-project-notifications-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_news_api_key_key "${VAULT_NAME}" "csm-cloud-project-news-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_news_api_key_secret "${VAULT_NAME}" "csm-cloud-project-news-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_activity_api_key_key "${VAULT_NAME}" "csm-cloud-project-activity-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_activity_api_key_secret "${VAULT_NAME}" "csm-cloud-project-activity-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_api_timeseries_api_key_key "${VAULT_NAME}" "csm-cloud-project-api-timeseries-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.project_api_timeseries_api_key_secret "${VAULT_NAME}" "csm-cloud-project-api-timeseries-kafka-broker-api-secret-${ENV_COLOR}"
    if ${RESET_ENABLED}; then
        import_key_vault_secret "azurerm_key_vault_secret.reset_api_key_key[0]" "${VAULT_NAME}" "csm-cloud-reset-kafka-broker-api-key-${ENV_COLOR}"
        import_key_vault_secret "azurerm_key_vault_secret.reset_api_key_secret[0]" "${VAULT_NAME}" "csm-cloud-reset-kafka-broker-api-secret-${ENV_COLOR}"
    fi
    import_key_vault_secret azurerm_key_vault_secret.bam_importer_api_key "${VAULT_NAME}" "csm-monitoring-metrics-influxdb-importer-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.bam_importer_api_secret "${VAULT_NAME}" "csm-monitoring-metrics-influxdb-importer-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.kafka_lag_exporter_api_key_key "${VAULT_NAME}" "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.kafka_lag_exporter_api_key_secret "${VAULT_NAME}" "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-secret-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.kafka_migrator_api_key_key "${VAULT_NAME}" "csm-cloud-kafka-migrator-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret azurerm_key_vault_secret.kafka_migrator_api_key_secret "${VAULT_NAME}" "csm-cloud-kafka-migrator-kafka-broker-api-secret-${ENV_COLOR}"
fi

import_key_vault_secret azurerm_key_vault_secret.readonly_api_key_key "${VAULT_NAME}" "csm-read-only-kafka-broker-api-key-${ENV_COLOR}"
import_key_vault_secret azurerm_key_vault_secret.readonly_api_key_secret "${VAULT_NAME}" "csm-read-only-kafka-broker-api-secret-${ENV_COLOR}"
import_key_vault_secret azurerm_key_vault_secret.delete_consumer_groups_api_key_key "${VAULT_NAME}" "csm-dev-del-cgp-kafka-broker-api-key-${ENV_COLOR}"
import_key_vault_secret azurerm_key_vault_secret.delete_consumer_groups_api_key_secret "${VAULT_NAME}" "csm-dev-del-cgp-kafka-broker-api-secret-${ENV_COLOR}"
if ${KAFKA_BACKUP_ENABLED}; then
    import_key_vault_secret "azurerm_key_vault_secret.kafka_backup_api_key_key[0]" "${VAULT_NAME}" "csm-cloud-backup-kafka-broker-api-key-${ENV_COLOR}"
    import_key_vault_secret "azurerm_key_vault_secret.kafka_backup_api_key_secret[0]" "${VAULT_NAME}" "csm-cloud-backup-kafka-broker-api-secret-${ENV_COLOR}"
fi

# Remove terraform state
rm -f ${TF_STATE}

echo -e "${GREENB}Successfully imported.${CLEAR}"

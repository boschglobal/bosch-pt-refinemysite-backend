#!/bin/bash

source functions.sh

ACTION="${1}"
ENVIRONMENT="${2}"
TOKEN="${3}"

if [[ -z ${ACTION} ]]; then
    echo "Failed to extract action."
    exit 2
fi

if [[ -z ${ENVIRONMENT} ]]; then
    echo "Failed to extract environment."
    exit 2
fi

if [[ -z ${TOKEN} ]]; then
    echo "Failed to extract token."
    exit 2
fi

case ${ACTION} in
add)
    MSG_TYPE="${4}"
    MSG_DE="${5}"
    MSG_EN="${6}"
    if [[ -z ${MSG_TYPE} ]]; then
        echo "Failed to extract msg_type."
        exit 2
    fi
    if [[ -z ${MSG_DE} ]]; then
        echo "Failed to extract msg_de."
        exit 2
    fi
    if [[ -z ${MSG_EN} ]]; then
        echo "Failed to extract msg_en."
        exit 2
    fi
    RESPONSE=$(set_maintenance_banner_by_token "${ENVIRONMENT}" "${TOKEN}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}")
    echo "$RESPONSE"
    STATUS=$(($(echo "$RESPONSE" | tail -n1)))
    if [[ ($STATUS -lt 200) || ($STATUS -gt 299) ]]; then
        echo "Expected success status code. Request failed with status $STATUS."
        exit 2
    fi
    ;;
list)
    get_ids_by_token "${ENVIRONMENT}" "${TOKEN}"
    ;;
delete)
    ID="${4}"
    if [[ -z ${ID} ]]; then
        echo "Failed to extract id."
        exit 2
    fi
    RESPONSE=$(delete_maintenance_banner_by_token "${ENVIRONMENT}" "${TOKEN}" "${ID}")
    echo "$RESPONSE"
    STATUS=$(($(echo "$RESPONSE" | tail -n1)))
    if [[ ($STATUS -lt 200) || ($STATUS -gt 299) ]]; then
        echo "Expected success status code. Request failed with status $STATUS."
        exit 2
    fi
    ;;
*)
    echo "Unexpected action: '${ACTION}'."
    exit 1
    ;;
esac

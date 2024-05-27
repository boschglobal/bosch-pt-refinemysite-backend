#!/bin/bash

source functions.sh

ACTION="${1}"
ENVIRONMENT="${2}"
COOKIE="${3}"

if [[ -z ${ACTION} ]]; then
    echo "Failed to extract action."
    exit 2
fi

if [[ -z ${ENVIRONMENT} ]]; then
    echo "Failed to extract environment."
    exit 2
fi

if [[ -z ${COOKIE} ]]; then
    echo "Failed to extract cookie."
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
    set_maintenance_banner_by_cookie "${ENVIRONMENT}" "${COOKIE}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}"
    ;;
list)
    get_ids_by_cookie "${ENVIRONMENT}" "${COOKIE}"
    ;;
delete)
    ID="${4}"
    if [[ -z ${ID} ]]; then
        echo "Failed to extract id."
        exit 2
    fi
    delete_maintenance_banner_by_cookie "${ENVIRONMENT}" "${COOKIE}" "${ID}"
    ;;
*)
    echo "Unexpected action: '${ACTION}'."
    exit 1
    ;;
esac

#!/bin/bash

__get_url_by_env() {
    local ENV_NAME=$(echo ${1} | awk '{print tolower($0)}')
    if [ "${ENV_NAME}" == "prod" ]; then
        local RMS_URL="https://app.bosch-refinemysite.com/api/v1/announcements"
    elif [ "${ENV_NAME}" == "local" ]; then
        local RMS_URL="http://localhost:8090/v1/announcements"
    else
        local RMS_URL="https://${ENV_NAME}.bosch-refinemysite.com/api/v1/announcements"
    fi
    echo "${RMS_URL}"
}

__set_maintenance_banner() {
    local RMS_URL=$(__get_url_by_env ${1})
    local RMS_AUTH="${2}"
    local RMS_MSG_TYPE="${3}"
    local RMS_MSG_DE="${4}"
    local RMS_MSG_EN="${5}"
    curl --http1.1 -s -w "%{http_code}\n" --location --request POST "${RMS_URL}" \
        -H 'Content-Type: application/json;charset=UTF-8' \
        -H 'Accept-Language: en' \
        -H 'Accept: application/hal+json' \
        -H "${RMS_AUTH}" \
        --data @- <<DATA | jq
{
    "type" : "${RMS_MSG_TYPE}",
    "translations" : [
        {
            "locale" : "de",
            "value" : "${RMS_MSG_DE}"
        },
        {
            "locale" : "en",
            "value" : "${RMS_MSG_EN}"
        }
    ]
}
DATA
}

set_maintenance_banner_by_token() {
    __set_maintenance_banner "${1}" "Authorization: Bearer ${2}" "${3}" "${4}" "${5}"
}

set_maintenance_banner_by_cookie() {
    __set_maintenance_banner "${1}" "Cookie: bosch-rms-auth-session=${2}" "${3}" "${4}" "${5}"
}

__get_ids() {
    local RMS_URL=$(__get_url_by_env ${1})
    local RMS_AUTH="${2}"
    curl --http1.1 -s -w "%{http_code}\n" --location --request GET "${RMS_URL}" \
        -H 'Accept: application/hal+json' \
        -H 'Accept-Language: en' \
        -H "${RMS_AUTH}" | jq
}

get_ids_by_token() {
    __get_ids "${1}" "Authorization: Bearer ${2}"
}

get_ids_by_cookie() {
    __get_ids "${1}" "Cookie: bosch-rms-auth-session=${2}"
}

__delete_maintenance_banner() {
    local RMS_URL=$(__get_url_by_env ${1})
    local RMS_AUTH="${2}"
    local RMS_ID="${3}"
    curl --http1.1 -s -w "%{http_code}\n" --location --request DELETE "${RMS_URL}/${RMS_ID}" \
        -H 'Accept: application/hal+json' \
        -H 'Accept-Language: en' \
        -H "${RMS_AUTH}" | jq
}

delete_maintenance_banner_by_token() {
    __delete_maintenance_banner "${1}" "Authorization: Bearer ${2}" "${3}"
}

delete_maintenance_banner_by_cookie() {
    __delete_maintenance_banner "${1}" "Cookie: bosch-rms-auth-session=${2}" "${3}"
}

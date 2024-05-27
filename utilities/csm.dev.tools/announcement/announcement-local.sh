#!/bin/bash

# This script is for local usage.

ENVIRONMENT="sandbox3"
MSG_TYPE="NEUTRAL"
MSG_DE="Derzeit sind keine PDF-Exporte möglich. Wir arbeiten daran, das Problem zu beheben."
MSG_EN="Currently, PDF exports are not available. We are working on fixing the issue."
ID="2753bad2-0de0-4c06-b261-296b9a8bf62a"

TOKEN="ttt"
#./announcement-token.sh add "${ENVIRONMENT}" "${TOKEN}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}"
#./announcement-token.sh list "${ENVIRONMENT}" "${TOKEN}"
#./announcement-token.sh delete "${ENVIRONMENT}" "${TOKEN}" "${ID}"

COOKIE="ccc"
#./announcement-cookie.sh add "${ENVIRONMENT}" "${COOKIE}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}"
#./announcement-cookie.sh list "${ENVIRONMENT}" "${COOKIE}"
#./announcement-cookie.sh delete "${ENVIRONMENT}" "${COOKIE}" "${ID}"

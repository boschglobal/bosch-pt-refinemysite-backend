#!/bin/bash

# Query Github for latest release, returning a JSON response
echo -n "Retrieving latest agent release from Github... "
RESPONSE_JSON=$(curl -sL https://api.github.com/repos/inspectIT/inspectit-ocelot/releases/latest)
echo "done"

# Get release name from JSON response
RELEASE_NAME=$(echo "${RESPONSE_JSON}" | jq -r '.name')
echo "Found latest release: ${RELEASE_NAME}"

# Filter JSON for agent file descriptor
AGENT_JSON=$(echo "${RESPONSE_JSON}" | jq -r '.assets[] | select(.name | contains("agent"))')

# Get download url from JSON response and initiate the agent download
echo "Downloading agent..."
AGENT_URL=$(echo "${AGENT_JSON}" | jq -r '.browser_download_url')
wget -q --show-progress "${AGENT_URL}"
echo "Download finished."
echo
echo "IMPORTANT: To complete the setup, follow these instructions:"

# Print instructions on setting up environment variables
AGENT_FILE=$(echo "${AGENT_JSON}" | jq -r '.name')
AGENT_PATH=$(readlink -f ${AGENT_FILE})
CONFIG_PATH=$(readlink -f config)
echo
echo "# Add the following lines to your user profile. The location depends on your shell."
echo "# Typical locations include: ~/.bashrc or ~./zshrc"
echo "#"
echo "# On macOS you need to construct the environment variables on your own because readlink -f fails."
echo
echo "export INSPECTIT_AGENT=\"${AGENT_PATH}\""
echo "export INSPECTIT_CONFIG=\"${CONFIG_PATH}/\""

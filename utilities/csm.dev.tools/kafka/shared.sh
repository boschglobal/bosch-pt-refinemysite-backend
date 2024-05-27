#!/bin/bash

set -e

DEV_SUBSCRIPTION_ID="REPLACE_ME"
QA_SUBSCRIPTION_ID="REPLACE_ME"
PROD_SUBSCRIPTION_ID="REPLACE_ME"
SANDBOX_SUBSCRIPTION_ID="REPLACE_ME"

# Confirm question, question has to be provided as function parameter
# e.g. "Continue creating Kafka service accounts and ACLs? (y/n) "
confirm() {
  QUESTION="${1}"
  while true; do
    read -p "$QUESTION" YES_OR_NO
    case $YES_OR_NO in
      [Yy]* ) break;;
      [Nn]* ) exit 0;;
      * ) echo "Please answer (y)es or (n)o.";;
    esac
  done
}

# Ask user for environment name to be used
select_env() {
  while true; do
    echo
    read -p "Choose name of environment (e.g. sandbox1): " ENV
    case $ENV in
      "dev" ) ENV_SHORT="dev"; ENV_TYPE="staging"; break;;
      "test1" ) ENV_SHORT="t1"; ENV_TYPE="staging"; break;;
      "review" ) ENV_SHORT="rev"; ENV_TYPE="prod"; break;;
      "prod" ) ENV_SHORT="prod"; ENV_TYPE="prod"; break;;
      "sandbox1" ) ENV_SHORT="s1"; ENV_TYPE="staging"; break;;
      "sandbox2" ) ENV_SHORT="s2"; ENV_TYPE="staging"; break;;
      "sandbox3" ) ENV_SHORT="s3"; ENV_TYPE="staging"; break;;
      "sandbox4" ) ENV_SHORT="s4"; ENV_TYPE="staging"; break;;
      * ) echo "Unknown environment name entered.";;
    esac
  done
}

# Ask user for blue or green setup
select_blue_green() {
  while true; do
    echo
    read -p "Select blue or green setup (blue/green): " BLUE_GREEN
    case $BLUE_GREEN in
      "blue" ) BLUE_GREEN_SHORT="blu"; break;;
      "green" ) BLUE_GREEN_SHORT="gre"; break;;
      * ) echo "Please enter 'blue' or 'green'.";;
    esac
  done
}

set_environment() {
  ENVIRONMENT=$1
  case $ENVIRONMENT in
    "dev")
      ENV_SHORT="dev"
      SUBSCRIPTION="$DEV_SUBSCRIPTION_ID";;
    "test1")
      ENV_SHORT="t1"
      SUBSCRIPTION="$QA_SUBSCRIPTION_ID";;
    "review")
      ENV_SHORT="rev"
      SUBSCRIPTION="$QA_SUBSCRIPTION_ID";;
    "prod")
      ENV_SHORT="prod"
      SUBSCRIPTION="$PROD_SUBSCRIPTION_ID";;
    "sandbox1")
      ENV_SHORT="s1"
      SUBSCRIPTION="$SANDBOX_SUBSCRIPTION_ID";;
    "sandbox2")
      ENV_SHORT="s2"
      SUBSCRIPTION="$SANDBOX_SUBSCRIPTION_ID";;
    "sandbox3")
      ENV_SHORT="s3"
      SUBSCRIPTION="$SANDBOX_SUBSCRIPTION_ID";;
    *) echo "Unknown environment: $1"; exit 1;;
  esac

  # some scripts use ENV instead of ENVIRONMENT
  ENV=$ENVIRONMENT

  ENV_TYPE="staging"
  if [[ $ENVIRONMENT == "prod" || $ENVIRONMENT == "review" ]]; then
    ENV_TYPE="prod"
  fi
}

set_color() {
  BLUE_GREEN=$1
  case $BLUE_GREEN in
    "blue" ) BLUE_GREEN_SHORT="blu";;
    "green" ) BLUE_GREEN_SHORT="gre";;
    * ) echo "Unknown color: ${BLUE_GREEN}"; exit 1;;
  esac
}

init_environment_and_color_from_args() {
  if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <dev|review|sandbox1|sandbox2|sandbox3|prod> <blue|green>" 1>&2
    exit 1
  fi

  ENVIRONMENT=$1
  set_environment $ENVIRONMENT
  echo "Using environment: ${ENV} (short: ${ENV_SHORT}, type: ${ENV_TYPE})"

  BLUE_GREEN=$2
  set_color $BLUE_GREEN
  echo "Using color: ${BLUE_GREEN} (short: ${BLUE_GREEN_SHORT})"
}

init_environment_from_args() {
  if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <dev|review|sandbox1|sandbox2|sandbox3|prod>" 1>&2
    exit 1
  fi

  ENVIRONMENT=$1
  set_environment $ENVIRONMENT
  echo "Using environment: ${ENV} (short: ${ENV_SHORT}, type: ${ENV_TYPE})"
}

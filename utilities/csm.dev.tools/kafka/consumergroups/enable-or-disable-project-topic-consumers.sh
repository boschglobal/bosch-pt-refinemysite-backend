#!/bin/bash
#
# Enables or disables consumers of the project topic by disabling the Kafka 
# listeners in the various services.
#
# This is done by adding/removing the "kafka-listener-disabled" profile 
# to/from the SPRING_PROFILES_ACTIVE environment variable.

set -e

PROJECT_TOPIC_CONSUMERS="
csm-cloud-project
csm-cloud-project-activity
csm-cloud-project-api-timeseries
csm-cloud-project-news
csm-cloud-project-notifications
csm-cloud-project-statistics
csm-monitoring-metrics-influxdb-importer"
# csm-cloud-project-restore-db is also a project topic consumer, but it normally doesn't run
# csm-cloud-kafka-backup is also a project topic consumer, but typically we don't disable its listeners

prompt_enable_or_disable() {
  while true; do
    read -p "Enable or disable Kafka listeners? (e/d) " ENABLE_OR_DISABLE
    case $ENABLE_OR_DISABLE in
    [Ee]* ) echo "enable"; break;;
    [Dd]* ) echo "disable"; break;;
    * ) echo "Please enter (e)nable or (d)isable";;
    esac
  done
}

deployment_exists() {
  local DEPLOYMENT=$1

  echo $(kubectl get deployments -l "app=$DEPLOYMENT" -o name -A)
}

get_namespace_of_deployment() {
  local DEPLOYMENT=$1

  echo $(kubectl get deployments \
    -l "app=$DEPLOYMENT" \
    -o jsonpath='{.items[*].metadata.namespace}' -A)
}

# adds the given profile to the existing SPRING_PROFILES_ACTIVE environment variable
add_active_profile() {
  local DEPLOYMENT=$1
  local PROFILE_TO_ADD=$2

  local NAMESPACE=$(get_namespace_of_deployment $DEPLOYMENT)
  local ACTIVE_PROFILES=$(get_active_profiles $DEPLOYMENT)

  if [[ $ACTIVE_PROFILES == *"$PROFILE_TO_ADD"* ]]; then
    echo "Profile '$PROFILE_TO_ADD' is already active. Skipping."
  else
    echo "Adding profile '$PROFILE_TO_ADD'"
    kubectl set env deployment/$DEPLOYMENT \
      "SPRING_PROFILES_ACTIVE=$ACTIVE_PROFILES,$PROFILE_TO_ADD" \
      -n $NAMESPACE
  fi
}

# removes the given profile from SPRING_PROFILES_ACTIVE environment variable
remove_active_profile() {
  local DEPLOYMENT=$1
  local PROFILE_TO_REMOVE=$2

  local NAMESPACE=$(get_namespace_of_deployment $DEPLOYMENT)
  local ACTIVE_PROFILES=$(get_active_profiles $DEPLOYMENT)
  local ACTIVE_PROFILES_NEW=$(sed "s/,$PROFILE_TO_REMOVE//g" <<<$ACTIVE_PROFILES)
  
  if [[ $ACTIVE_PROFILES == $ACTIVE_PROFILES_NEW ]]; then
    echo "Profile '$PROFILE_TO_REMOVE' is already inactive. Skipping."
  else
    echo "Removing profile '$PROFILE_TO_REMOVE'"
    kubectl set env deployment/$DEPLOYMENT \
      "SPRING_PROFILES_ACTIVE=$ACTIVE_PROFILES_NEW" \
      -n $NAMESPACE
  fi
}

get_active_profiles() {
  local DEPLOYMENT=$1
  
  local NAMESPACE=$(get_namespace_of_deployment $DEPLOYMENT) 
  echo $(kubectl get deployment/$DEPLOYMENT \
    -o jsonpath='{.spec.template.spec.containers[0].env[?(.name=="SPRING_PROFILES_ACTIVE")].value}' \
    -n $NAMESPACE)
}

print_active_profiles() {
  local DEPLOYMENT=$1
  
  local NAMESPACE=$(get_namespace_of_deployment $DEPLOYMENT)
  echo "Profiles active: $(get_active_profiles $DEPLOYMENT)"
}

wait_for_pods_ready() {
  DEPLOYMENT=$1
  NAMESPACE=$(get_namespace_of_deployment $DEPLOYMENT)

  # wait for deployment to be finished (this command is blocking)
  # without this command, the following wait command might return too early if there are no pods yet in the deployment
  kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE

  # wait for the pod to be ready
  kubectl wait --for=condition=ready pod -l app=$DEPLOYMENT --timeout=600s -A
}

ENABLE_OR_DISABLE=$(prompt_enable_or_disable)
for DEPLOYMENT in $PROJECT_TOPIC_CONSUMERS; do
  echo
  echo "Deployment: $DEPLOYMENT"
  if [ $(deployment_exists $DEPLOYMENT) ]; then
    print_active_profiles $DEPLOYMENT
    if [[ $ENABLE_OR_DISABLE == "enable" ]]; then
      remove_active_profile $DEPLOYMENT "kafka-listener-disabled"
    else
      add_active_profile $DEPLOYMENT "kafka-listener-disabled"
    fi
    print_active_profiles $DEPLOYMENT
  else
    echo "Deployment not found. Skipping."
  fi
  echo
  echo "-------------------------------------------------"
done

echo
echo "Waiting until the listeners are ${ENABLE_OR_DISABLE}d..."
for DEPLOYMENT in $PROJECT_TOPIC_CONSUMERS; do
  wait_for_pods_ready $DEPLOYMENT
done

echo
echo "Done."
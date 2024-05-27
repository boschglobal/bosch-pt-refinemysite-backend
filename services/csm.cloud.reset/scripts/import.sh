#!/bin/bash

set -e

ENVIRONMENT=$1
DATASET=$2

if [[ -z $ENVIRONMENT ]]; then
    echo "Please specify the target environment."
    echo "Possible values are: dev, review, sandbox1, sandbox2, sandbox4, sandbox4"
    echo -n "> "
    read ENVIRONMENT
fi

if [[ -z $DATASET ]]; then
    echo "Please specify the dataset to import."
    echo "Possible values are: aat, base, loadtest, review, testdata, usertest"
    echo -n "> "
    read DATASET
fi

kubectl config use-context pt-csm-$ENVIRONMENT-aks

echo "Resetting databases and recreating kafka topics..."
STATUSCODE=`curl -X POST --write-out %{http_code} --silent --output /dev/null https://pt-csm-$ENVIRONMENT.westeurope.cloudapp.azure.com/api/reset`
if [[ "$STATUSCODE" -ne 200 ]] ; then
    echo "Reset request failed with status $STATUSCODE"
    exit 1
fi

echo "Restarting pods that are connected to a kafka topic..."
kubectl delete pod -l 'app in (csm-cloud-project-activities, csm-cloud-project-news, csm-cloud-project-statistics, csm-cloud-project-kafka-connector)'

echo "Importing dataset $DATASET..."
STATUSCODE=`curl -X POST --write-out %{http_code} --silent --output /dev/null https://pt-csm-$ENVIRONMENT.westeurope.cloudapp.azure.com/api/import -d dataset=$DATASET`
if [[ "$STATUSCODE" -ne 200 ]] ; then
    echo "Reset request failed with status $STATUSCODE"
    exit 1
fi

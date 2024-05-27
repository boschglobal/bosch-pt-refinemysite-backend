#!/bin/bash
#
# Scales up all services listening to the project topic.
#
# If another topic is being migrated, this script needs adjustments.

set -e

# kubectl scale --replicas=1 deployment/csm-cloud-project-restore-db
# kubectl scale --replicas=1 deployment/csm-cloud-company-restore-db
# kubectl scale --replicas=1 deployment/csm-cloud-user-restore-db

kubectl scale --replicas=2 deployment/csm-cloud-project-activity
kubectl scale --replicas=2 deployment/csm-cloud-project-news
kubectl scale --replicas=2 deployment/csm-cloud-project-notifications
kubectl scale --replicas=2 deployment/csm-cloud-project-statistics

kubectl scale --replicas=1 deployment/csm-monitoring-metrics-influxdb-importer -n monitoring

kubectl scale --replicas=1 deployment/csm-cloud-kafka-backup

echo "Done."

#!/bin/sh

echo "export CLUSTER_ID=$(kafka-storage random-uuid)" >> /etc/confluent/docker/bash-config
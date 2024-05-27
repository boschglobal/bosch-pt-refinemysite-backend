#!/bin/bash

echo "###############";
echo "#   VERSION   #";
echo "###############";
terraform version;

echo "################";
echo "#   PROVIDER   #";
echo "################";
terraform providers;

echo "#############";
echo "#   STATE   #";
echo "#############";
terraform state list;

echo "##############";
echo "#   OUTPUT   #";
echo "##############";
terraform output;

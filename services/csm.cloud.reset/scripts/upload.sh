#!/bin/bash

echo -n "Source Folder: "
read SOURCE

echo -n "Destination Path: "
read DEST

az storage blob upload-batch --subscription REPLACE_ME --account-name ptcsmtestdata \
    --source $SOURCE \
    --destination pt-csm-testdata \
    --destination-path $DEST

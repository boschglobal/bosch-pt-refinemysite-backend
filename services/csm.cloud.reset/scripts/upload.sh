#!/bin/bash

echo -n "Source Folder: "
read SOURCE

echo -n "Destination Path: "
read DEST

az storage blob upload-batch --subscription 96aa5728-e64f-4ee5-9fd8-e656f878aa03 --account-name ptcsmtestdata \
    --source $SOURCE \
    --destination pt-csm-testdata \
    --destination-path $DEST

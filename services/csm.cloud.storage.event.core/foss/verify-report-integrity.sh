#!/bin/bash

source config.sh

# Ensure go-licenses is a local executable to avoid installation path issues in pipelines
GOBIN=$(pwd) ./install-license-tool.sh

# Generate a new license report
./go-licenses report ../ --ignore "$IGNORED_PACKAGES" >report/current-licenses.csv

# Calculate the difference between the new license report and the one checked in
differences=$(diff report/current-licenses.csv report/licenses.csv)

# Delete the newly generated report and the local executable (clean up)
rm go-licenses
rm report/current-licenses.csv

# Return an error if there are differences
if [ "$differences" != "" ]; then
  echo "There are differences in foss/report/licenses.csv and a newly generated license report $differences."
  echo "Please verify the license report is up-to-date (run update-report.sh, check and commit licenses.csv)"
  exit 1
else
  echo "License report is up to date."
fi

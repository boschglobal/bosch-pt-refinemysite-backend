#!/bin/bash

source config.sh

echo ""
echo "Generating license report"
mkdir -p report
go-licenses report ../ --ignore "$IGNORED_PACKAGES" >report/licenses.csv

echo ""
echo "Downloading and saving licenses in report/licenses"
go-licenses save ../ --ignore "$IGNORED_PACKAGES" --save_path=report/licenses

echo ""
echo "Displaying licenses that may warrant a second look at:"
# warning about notice typed licenses would output MIT and BSD-3-Clause as well
go-licenses check ../ --ignore "$IGNORED_PACKAGES" --disallowed_types=forbidden,reciprocal,restricted,unencumbered,notice

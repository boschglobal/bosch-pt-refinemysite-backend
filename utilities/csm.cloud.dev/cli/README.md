# Smartsite CLI

The Smartsite CLI is a script to simplify the handling of the vast number of git
repositories.

## Prerequisites

Python and pip are required to run the app and install required libraries.

## Setup

Copy and save the conf.json.template file in the smartsite-cli folder as conf.json.
Enter your username / personal access token from azure dev-ops into the
"username" and "token" attributes.
If your git directory is not located at "/home/developer/Projects" adjust the
directory attribute.
To not clone a specific repository from azure dev-ops or to exclude it from
operations provided by the cli, add it to the blacklist.
The conf.json is excluded in the .gitignore file, therefore no personal data is
exposed.
Ensure that the personal access token has the permission "Code (Read)" (configurable
in azure dev ops).

## Usage

Run `./smartsite-cli.py` to see the documentation of the cli.

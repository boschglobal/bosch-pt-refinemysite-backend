# Introduction

This is a repository for convenience scripts.

## Terraform config

`terraform/create-terraform-config.sh` is a script to quickly set up the configuration for Terraform. It creates Terraform configuration files by loading secrets from key vaults and writing them to the configuration.

Usage:

- `./create-terraform-config.sh -s <subscription>`

Example:

- `./create-terraform-config.sh -s sandbox`

## Announcement

In `announcment/*` are scripts to `add`, `list` and `delete` announcements on the RefinemySite Platform to inform customers about ongoing things. This could be issues with services, network problems or any other important information for customers.

### Use in pipeline

There is a pipeline [Announcement](https://dev.azure.com/pt-iot/smartsite/_build?definitionId=1545) in Azure DevOps which can be used to `add`, `list` and `delete` announcements. The pipeline uses the announcement user `smartsiteapp+announcement@gmail.com` to create a RefinemySite bearer token. After that the script `announcement/announcement-token.sh` is called to add, list or delete the announcements. Only if the pipeline is called with the action `add` and a `ttl` greater than `0`, the pipeline adds an announcement and runs a [ManualValidation](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/manual-validation-v0?view=azure-pipelines) to pause the pipeline until end of life is reached and resumes it again. Then the pipeline calls itself with the action `delete` to delete the announcement.

The pipelines uses the following parameters:

- ACTION - The action to call on the announcement. Valid values are `add`, `list` and `delete`.
- ENV - The environment on which the announcement should be applied. E. g. `sandbox2`.
- TTL - The time to life in minutes of the announcement. Defaults to `0`, which means no end of life. Only required for the action `add`.
- MSG_TYPE - The message type (colored in red, blue, green or yellow). Valid values are `ERROR`, `NEUTRAL`, `WARN`, and `SUCCESS`. Only required for the action `add`.
- MSG_DE - The german translation of the message. Only required for the action `add`.
- MSG_EN - The english translation of the message. Only required for the action `add`.
- ID - The ID of the announcement to delete. Only required for the action `delete`.

### Local usage

Use the script `announcment/announcement-local.sh` if you want to `add`, `list` and `delete` announcements from your local machine.

Usage with RefinemySite bearer `token` (must be generated with Postman or oauth-utilities):

```bash
# Add an announcement
./announcement-token.sh add "${ENVIRONMENT}" "${TOKEN}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}"
# List announcements (prints a list of announcements with ID)
./announcement-token.sh list "${ENVIRONMENT}" "${TOKEN}"
# Delete an announcement by its ID (get ID from list)
./announcement-token.sh delete "${ENVIRONMENT}" "${TOKEN}" "${ID}"
```

Usage with RefinemySite `cookie` (must be taken from your browser after login to RefinemySite by using the Web Developer Tools):

```bash
# Add an announcement
./announcement-cookie.sh add "${ENVIRONMENT}" "${COOKIE}" "${MSG_TYPE}" "${MSG_DE}" "${MSG_EN}"
# List announcements (prints a list of announcements with ID)
./announcement-cookie.sh list "${ENVIRONMENT}" "${COOKIE}"
# Delete an announcement by its ID (get ID from list)
./announcement-cookie.sh delete "${ENVIRONMENT}" "${COOKIE}" "${ID}"
```

### Example messages

```bash
MSG_DE: "Aufgrund eines Netzwerkproblems reagiert unser System nur langsam. Wir entschuldigen uns für die Unannehmlichkeiten und danken Ihnen für Ihr Verständnis."
MSG_EN: "Due to a network problem we experience slow responses of our system. We apologize for the inconvenience and thank you for your understanding."

MSG_DE: "Am Samstag, den 20. November, 12:00 – 12:30 Uhr CET, wird die RefinemySite Plattform aufgrund geplanter Wartungsarbeiten nicht verfügar sein. Wir entschuldigen uns für die Unannehmlichkeiten und danken Ihnen für Ihr Verständnis."
MSG_EN: "On Saturday, November 20, 12:00 - 12:30 pm CET, the RefinemySite platform will not be available due to planned maintenance. We apologize for the inconvenience and thank you for your understanding."

MSG_DE: "Am Samstag, den 06. August, 14:00 – 14:30 Uhr CET, wird die RefinemySite Plattform aufgrund geplanter Wartungsarbeiten nur eingeschränkt nutzbar sein. Wir entschuldigen uns für die Unannehmlichkeiten und danken Ihnen für Ihr Verständnis."
MSG_EN: "On Saturday, August 06, 14:00 - 14:30 CET, the RefinemySite platform will be of limited use due to planned maintenance. We apologize for the inconvenience and thank you for your understanding."

MSG_DE: "Derzeit sind keine PDF-Exporte möglich. Wir arbeiten daran, das Problem zu beheben."
MSG_EN: "Currently, PDF exports are not available. We are working on fixing the issue."
```

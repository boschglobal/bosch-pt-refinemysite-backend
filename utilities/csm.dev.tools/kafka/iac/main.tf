# Azurerm
variable "subscription_id" {
  type        = string
  description = "The Azure subscription ID"
}

variable "tenant_id" {
  type        = string
  description = "The Azure tenant ID"
}

variable "client_id" {
  type        = string
  description = "The Azure client ID"
}

variable "client_secret" {
  type        = string
  description = "The Azure client secret access key"
}

# Environment
variable "env" {
  type        = string
  description = "Environment to use"
}

# Confluent Cloud
variable "confluent_cloud_api_secret" {
  type        = string
  description = "The confluent cloud API secret"
}

variable "confluent_cloud_api_key" {
  type        = string
  description = "The confluent cloud API secret"
}

variable "confluent_color" {
  type        = string
  description = "The color of confluent resources"
}

variable "confluent_color_short" {
  type        = string
  description = "The color of confluent resources in short form"
}

# Cluster
variable "is_backup_cluster" {
  type        = bool
  description = "Configure the cluster as a backup cluster with create and write permissions. Otherwise read only."
  default     = false
}

variable "services_enabled" {
  type        = bool
  description = "Create service accounts, api keys and ACL's for all services. The following services are excluded: kafka-backup, readonly and del-cgp."
  default     = true
}

# Create, rotate and delete API Keys
variable "primary_api_key_enabled" {
  type        = bool
  description = "Configure if the primary API Key is enabled."
}

variable "secondary_api_key_enabled" {
  type        = bool
  description = "Configure if the secondary API Key is enabled."
}

variable "active_api_key" {
  type        = string
  description = "Configure which API Key is active, the primary or secondary. Use 'primary' or 'secondary' as values."
}

locals {
  # Resource Group of Key Vaults
  kafka_aks_kv_rg_name = "pt-csm-${var.env}-kafka-aks-kv"

  # Normalize Key Vault name by removing anything except of letters and numbers
  kafka_aks_kv_normalized_name = replace(local.kafka_aks_kv_rg_name, "/[^A-Za-z0-9]/", "")

  is_env_prod = lower(var.env) == "prod"

  env_short = {
    dev      = "dev"
    prod     = "prod"
    review   = "rev"
    sandbox1 = "s1"
    sandbox2 = "s2"
    sandbox3 = "s3"
    sandbox4 = "s4"
    test1    = "t1"
  }

  # Topic configuration
  one_week_in_ms     = "604800000"
  ten_minutes_in_ms  = "600000"
  infinite_retention = "-1"
}

data "azurerm_key_vault" "kafka_aks_kv" {
  name                = local.kafka_aks_kv_normalized_name
  resource_group_name = local.kafka_aks_kv_rg_name
}

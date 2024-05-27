variable "confluent_cluster_name" {
  type        = string
  description = "Name of the confluent cluster"
}

variable "cluster_type" {
  type        = string
  description = "What type of cluster should be used? Currently only 'Basic' or 'Standard' clusters are used."
  default     = "Standard"
}

variable "confluent_cluster_availability" {
  type        = string
  description = "The availability zone configuration of the Kafka cluster. Accepted values are: SINGLE_ZONE and MULTI_ZONE. If the cluster is of type 'Basic', only SINGLE_ZONE is available and used."
  default     = "MULTI_ZONE"
}

resource "confluent_kafka_cluster" "this" {
  display_name = var.confluent_cluster_name
  availability = lower(var.cluster_type) == "basic" ? "SINGLE_ZONE" : var.confluent_cluster_availability
  cloud        = "AZURE"
  region       = "westeurope"

  dynamic "basic" {
    for_each = lower(var.cluster_type) == "basic" ? ["Basic"] : []
    content {}
  }

  dynamic "standard" {
    for_each = lower(var.cluster_type) == "standard" ? ["Standard"] : []
    content {}
  }

  environment {
    id = confluent_environment.this.id
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_key_vault_secret" "kafka_broker_urls" {
  name         = "kafka-broker-urls-${var.confluent_color}"
  value        = confluent_kafka_cluster.this.bootstrap_endpoint
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id
}

# Terraform App Manager (CloudClusterAdmin) that creates and manages Topics, Service Accounts, Api Keys and ACL's in the Confluent Kafka Cluster
resource "confluent_service_account" "terraform" {
  display_name = "csm-${local.env_short[var.env]}-terraform-${var.confluent_color_short}"
  description  = "Service Account that creates topics and ACLs in the cluster ${var.confluent_cluster_name}"

  lifecycle {
    prevent_destroy = true
  }
}

resource "confluent_role_binding" "terraform" {
  principal   = "User:${confluent_service_account.terraform.id}"
  role_name   = "CloudClusterAdmin"
  crn_pattern = confluent_kafka_cluster.this.rbac_crn

  lifecycle {
    prevent_destroy = true
  }
}

resource "confluent_api_key" "terraform" {
  depends_on = [confluent_role_binding.terraform]

  display_name = confluent_service_account.terraform.display_name
  description  = "API Key owned by service account ${confluent_service_account.terraform.display_name}"

  owner {
    api_version = confluent_service_account.terraform.api_version
    id          = confluent_service_account.terraform.id
    kind        = confluent_service_account.terraform.kind
  }

  managed_resource {
    id          = confluent_kafka_cluster.this.id
    api_version = confluent_kafka_cluster.this.api_version
    kind        = confluent_kafka_cluster.this.kind

    environment {
      id = confluent_environment.this.id
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

variable "project_topic_partitions_count" {
  type        = number
  description = "Project topic's partitions count"
}

variable "project_topic_version_suffix" {
  type        = string
  description = "Project topic's version suffix"
  default     = ""
}

variable "project_delete_topic_partitions_count" {
  type        = number
  description = "Project Delete topic's partitions count"
}

variable "project_delete_topic_version_suffix" {
  type        = string
  description = "Project Delete topic's version suffix"
  default     = ""
}

variable "project_invitation_topic_partitions_count" {
  type        = number
  description = "Project Invitation topic's partitions count"
}

variable "project_invitation_topic_version_suffix" {
  type        = string
  description = "Project Invitation topic's version suffix"
  default     = ""
}

resource "confluent_service_account" "project" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-project-${var.confluent_color_short}"
  description  = "${var.env} Project Service ${var.confluent_color}"
}

resource "confluent_api_key" "project_primary" {
  count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.project[0].display_name
  description  = "${var.env} Project Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.project[0].id
    api_version = confluent_service_account.project[0].api_version
    kind        = confluent_service_account.project[0].kind
  }

  managed_resource {
    id          = confluent_kafka_cluster.this.id
    api_version = confluent_kafka_cluster.this.api_version
    kind        = confluent_kafka_cluster.this.kind

    environment {
      id = confluent_environment.this.id
    }
  }
}

resource "confluent_api_key" "project_secondary" {
  count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.project[0].display_name
  description  = "${var.env} Project Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.project[0].id
    api_version = confluent_service_account.project[0].api_version
    kind        = confluent_service_account.project[0].kind
  }

  managed_resource {
    id          = confluent_kafka_cluster.this.id
    api_version = confluent_kafka_cluster.this.api_version
    kind        = confluent_kafka_cluster.this.kind

    environment {
      id = confluent_environment.this.id
    }
  }
}

resource "azurerm_key_vault_secret" "project_api_key_key" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-project-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.project_primary[0].id : confluent_api_key.project_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "project_api_key_secret" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-project-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.project_primary[0].secret : confluent_api_key.project_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_topic" "project" {
  topic_name       = "csm.${var.env}.projectmanagement.project${var.project_topic_version_suffix}"
  partitions_count = var.project_topic_partitions_count
  rest_endpoint    = confluent_kafka_cluster.this.rest_endpoint

  config = {
    "cleanup.policy"      = "delete"
    "retention.bytes"     = local.infinite_retention
    "retention.ms"        = local.infinite_retention
    "min.insync.replicas" = "2"
  }

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "confluent_kafka_topic" "project_delete" {
  topic_name       = "csm.${var.env}.projectmanagement.project.delete${var.project_delete_topic_version_suffix}"
  partitions_count = var.project_delete_topic_partitions_count
  rest_endpoint    = confluent_kafka_cluster.this.rest_endpoint

  config = {
    "cleanup.policy"      = "delete"
    "retention.bytes"     = local.infinite_retention
    "retention.ms"        = local.one_week_in_ms
    "min.insync.replicas" = "2"
  }

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "confluent_kafka_topic" "project_invitation" {
  topic_name       = "csm.${var.env}.projectmanagement.project.invitation${var.project_invitation_topic_version_suffix}"
  partitions_count = var.project_invitation_topic_partitions_count
  rest_endpoint    = confluent_kafka_cluster.this.rest_endpoint

  config = {
    "cleanup.policy"        = "compact"
    "retention.bytes"       = local.infinite_retention
    "retention.ms"          = local.infinite_retention
    "delete.retention.ms"   = local.one_week_in_ms
    "max.compaction.lag.ms" = local.one_week_in_ms
    "min.compaction.lag.ms" = "0"
    "min.insync.replicas"   = "2"
    "segment.ms"            = local.one_week_in_ms
  }

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "confluent_kafka_acl" "project_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm-pm-${var.env}"
  principal     = "User:${confluent_service_account.project[0].id}"
  host          = "*"
  rest_endpoint = confluent_kafka_cluster.this.rest_endpoint

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }
}

resource "confluent_kafka_acl" "project_topic_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.project[0].id}"
  host          = "*"
  rest_endpoint = confluent_kafka_cluster.this.rest_endpoint

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }
}

resource "confluent_kafka_acl" "project_topic_write_project_delete" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "WRITE"
  pattern_type  = "LITERAL"
  resource_name = confluent_kafka_topic.project_delete.topic_name
  principal     = "User:${confluent_service_account.project[0].id}"
  host          = "*"
  rest_endpoint = confluent_kafka_cluster.this.rest_endpoint

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }
}

resource "confluent_kafka_acl" "project_topic_write_job_command" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "WRITE"
  pattern_type  = "LITERAL"
  resource_name = confluent_kafka_topic.job_command.topic_name
  principal     = "User:${confluent_service_account.project[0].id}"
  host          = "*"
  rest_endpoint = confluent_kafka_cluster.this.rest_endpoint

  kafka_cluster {
    id = confluent_kafka_cluster.this.id
  }

  credentials {
    key    = confluent_api_key.terraform.id
    secret = confluent_api_key.terraform.secret
  }
}

variable "user_topic_partitions_count" {
  type        = number
  description = "User topic's partitions count"
}

variable "user_topic_version_suffix" {
  type        = string
  description = "User topic's version suffix"
  default     = ""
}

variable "user_consents_topic_partitions_count" {
  type        = number
  description = "User Consents topic's partitions count"
}

variable "user_consents_topic_version_suffix" {
  type        = string
  description = "User Consents topic's version suffix"
  default     = ""
}

variable "user_craft_topic_partitions_count" {
  type        = number
  description = "User Craft topic's partitions count"
}

variable "user_craft_topic_version_suffix" {
  type        = string
  description = "User Craft topic's version suffix"
  default     = ""
}

variable "user_pat_topic_partitions_count" {
  type        = number
  description = "User PAT topic's partitions count"
}

variable "user_pat_topic_version_suffix" {
  type        = string
  description = "User PAT topic's version suffix"
  default     = ""
}

resource "confluent_service_account" "user" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-user-${var.confluent_color_short}"
  description  = "${var.env} User Service ${var.confluent_color}"
}

resource "confluent_api_key" "user_primary" {
  count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.user[0].display_name
  description  = "${var.env} User Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.user[0].id
    api_version = confluent_service_account.user[0].api_version
    kind        = confluent_service_account.user[0].kind
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

resource "confluent_api_key" "user_secondary" {
  count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.user[0].display_name
  description  = "${var.env} User Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.user[0].id
    api_version = confluent_service_account.user[0].api_version
    kind        = confluent_service_account.user[0].kind
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

resource "azurerm_key_vault_secret" "user_api_key_key" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-user-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.user_primary[0].id : confluent_api_key.user_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "user_api_key_secret" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-user-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.user_primary[0].secret : confluent_api_key.user_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_topic" "user" {
  topic_name       = "csm.${var.env}.usermanagement.user${var.user_topic_version_suffix}"
  partitions_count = var.user_topic_partitions_count
  rest_endpoint    = confluent_kafka_cluster.this.rest_endpoint

  # Some configurations are different on prod and we assume this was done unintentionally
  config = {
    "cleanup.policy"                      = "compact"
    "retention.bytes"                     = local.infinite_retention
    "retention.ms"                        = local.infinite_retention
    "delete.retention.ms"                 = local.one_week_in_ms
    "max.compaction.lag.ms"               = local.one_week_in_ms
    "min.compaction.lag.ms"               = "0"
    "min.insync.replicas"                 = "2"
    "segment.bytes"                       = local.is_env_prod ? "104857600" : null # 100 MB
    "segment.ms"                          = local.one_week_in_ms
    "max.message.bytes"                   = local.is_env_prod ? "2097164" : null             # 2 MB + 12 Bytes, but we do not know why
    "message.timestamp.difference.max.ms" = local.is_env_prod ? "9223372036854775807" : null # This value corresponds to the max long and is the default value
    "message.timestamp.type"              = local.is_env_prod ? "CreateTime" : null          # CreateTime is the default value
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

resource "confluent_kafka_topic" "user_consents" {
  topic_name       = "csm.${var.env}.usermanagement.consents${var.user_consents_topic_version_suffix}"
  partitions_count = var.user_consents_topic_partitions_count
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

resource "confluent_kafka_topic" "user_craft" {
  topic_name       = "csm.${var.env}.referencedata.craft${var.user_craft_topic_version_suffix}"
  partitions_count = var.user_craft_topic_partitions_count
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

resource "confluent_kafka_topic" "user_pat" {
  topic_name       = "csm.${var.env}.usermanagement.pat${var.user_pat_topic_version_suffix}"
  partitions_count = var.user_pat_topic_partitions_count
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

resource "confluent_kafka_acl" "user_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm-um-${var.env}"
  principal     = "User:${confluent_service_account.user[0].id}"
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

resource "confluent_kafka_acl" "user_topic_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.user[0].id}"
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

variable "event_mobile_command_topic_partitions_count" {
  type        = number
  description = "Event Mobile Command topic's partitions count"
}

variable "event_mobile_command_topic_version_suffix" {
  type        = string
  description = "Event Mobile Command topic's version suffix"
  default     = ""
}

resource "confluent_service_account" "event_mobile" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-event-mobile-${var.confluent_color_short}"
  description  = "${var.env} Event Mobile Service ${var.confluent_color}"
}

resource "confluent_api_key" "event_mobile_primary" {
  # Remove Event Mobile API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0
  count = 0

  display_name = confluent_service_account.event_mobile[0].display_name
  description  = "${var.env} Event Mobile Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.event_mobile[0].id
    api_version = confluent_service_account.event_mobile[0].api_version
    kind        = confluent_service_account.event_mobile[0].kind
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

resource "confluent_api_key" "event_mobile_secondary" {
  # Remove Event Mobile API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0
  count = 0

  display_name = confluent_service_account.event_mobile[0].display_name
  description  = "${var.env} Event Mobile Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.event_mobile[0].id
    api_version = confluent_service_account.event_mobile[0].api_version
    kind        = confluent_service_account.event_mobile[0].kind
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

resource "azurerm_key_vault_secret" "event_mobile_api_key_key" {
  # Remove Event Mobile API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled ? 1 : 0
  count = 0

  name         = "csm-cloud-event-mobile-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.event_mobile_primary[0].id : confluent_api_key.event_mobile_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "event_mobile_api_key_secret" {
  # Remove Event Mobile API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled ? 1 : 0
  count = 0

  name         = "csm-cloud-event-mobile-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.event_mobile_primary[0].secret : confluent_api_key.event_mobile_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_topic" "event_mobile_command" {
  topic_name       = "csm.${var.env}.event.mobile.command${var.event_mobile_command_topic_version_suffix}"
  partitions_count = var.event_mobile_command_topic_partitions_count
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

resource "confluent_kafka_acl" "event_mobile_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm-cloud-event-mobile-${var.env}"
  principal     = "User:${confluent_service_account.event_mobile[0].id}"
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

resource "confluent_kafka_acl" "event_mobile_topic_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.event_mobile[0].id}"
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

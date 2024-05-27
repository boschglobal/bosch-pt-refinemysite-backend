variable "event_topic_partitions_count" {
  type        = number
  description = "Event topic's partitions count"
}

variable "event_topic_version_suffix" {
  type        = string
  description = "Event topic's version suffix"
  default     = ""
}

resource "confluent_service_account" "event" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-event-${var.confluent_color_short}"
  description  = "${var.env} Event Service ${var.confluent_color}"
}

resource "confluent_api_key" "event_primary" {
  count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.event[0].display_name
  description  = "${var.env} Event Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.event[0].id
    api_version = confluent_service_account.event[0].api_version
    kind        = confluent_service_account.event[0].kind
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

resource "confluent_api_key" "event_secondary" {
  count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.event[0].display_name
  description  = "${var.env} Event Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.event[0].id
    api_version = confluent_service_account.event[0].api_version
    kind        = confluent_service_account.event[0].kind
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

resource "azurerm_key_vault_secret" "event_api_key_key" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-event-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.event_primary[0].id : confluent_api_key.event_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "event_api_key_secret" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-event-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.event_primary[0].secret : confluent_api_key.event_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_topic" "event" {
  topic_name       = "csm.${var.env}.event${var.event_topic_version_suffix}"
  partitions_count = var.event_topic_partitions_count
  rest_endpoint    = confluent_kafka_cluster.this.rest_endpoint

  config = {
    "cleanup.policy"      = "delete"
    "retention.bytes"     = local.infinite_retention
    "retention.ms"        = local.ten_minutes_in_ms
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

resource "confluent_kafka_acl" "event_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm-event-${var.env}"
  principal     = "User:${confluent_service_account.event[0].id}"
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

resource "confluent_kafka_acl" "event_topic_read_event" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "LITERAL"
  resource_name = confluent_kafka_topic.event.topic_name
  principal     = "User:${confluent_service_account.event[0].id}"
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

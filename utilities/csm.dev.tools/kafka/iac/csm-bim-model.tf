variable "bim_model_topic_partitions_count" {
  type        = number
  description = "BIM Model topic's partitions count"
}

variable "bim_model_topic_version_suffix" {
  type        = string
  description = "BIM Model topic's version suffix"
  default     = ""
}

resource "confluent_service_account" "bim_model" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-bim-model-${var.confluent_color_short}"
  description  = "${var.env} BIM Model Service ${var.confluent_color}"
}

resource "confluent_api_key" "bim_model_primary" {
  # Remove BIM Model API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0
  count = 0

  display_name = confluent_service_account.bim_model[0].display_name
  description  = "${var.env} BIM Model Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.bim_model[0].id
    api_version = confluent_service_account.bim_model[0].api_version
    kind        = confluent_service_account.bim_model[0].kind
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

resource "confluent_api_key" "bim_model_secondary" {
  # Remove BIM Model API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0
  count = 0

  display_name = confluent_service_account.bim_model[0].display_name
  description  = "${var.env} BIM Model Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.bim_model[0].id
    api_version = confluent_service_account.bim_model[0].api_version
    kind        = confluent_service_account.bim_model[0].kind
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

resource "azurerm_key_vault_secret" "bim_model_api_key_key" {
  # Remove BIM Model API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled ? 1 : 0
  count = 0

  name         = "csm-cloud-bim-model-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.bim_model_primary[0].id : confluent_api_key.bim_model_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "bim_model_api_key_secret" {
  # Remove BIM Model API Keys to avoid reaching the 50 key quota during key rotation
  # count = var.services_enabled ? 1 : 0
  count = 0

  name         = "csm-cloud-bim-model-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.bim_model_primary[0].secret : confluent_api_key.bim_model_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_topic" "bim_model" {
  topic_name       = "csm.${var.env}.bim.model${var.bim_model_topic_version_suffix}"
  partitions_count = var.bim_model_topic_partitions_count
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

resource "confluent_kafka_acl" "bim_model_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm-cloud-bim-model-${var.env}"
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

resource "confluent_kafka_acl" "bim_model_topic_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

resource "confluent_kafka_acl" "bim_model_topic_write_bim_model" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "WRITE"
  pattern_type  = "LITERAL"
  resource_name = confluent_kafka_topic.bim_model.topic_name
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

resource "confluent_kafka_acl" "bim_model_topic_write_event" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "WRITE"
  pattern_type  = "LITERAL"
  resource_name = confluent_kafka_topic.event.topic_name
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

resource "confluent_kafka_acl" "bim_model_transactional_create" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TRANSACTIONAL_ID"
  operation     = "CREATE"
  pattern_type  = "PREFIXED"
  resource_name = "csm-cloud-bim-model-${var.env}"
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

resource "confluent_kafka_acl" "bim_model_transactional_write" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TRANSACTIONAL_ID"
  operation     = "WRITE"
  pattern_type  = "PREFIXED"
  resource_name = "csm-cloud-bim-model-${var.env}"
  principal     = "User:${confluent_service_account.bim_model[0].id}"
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

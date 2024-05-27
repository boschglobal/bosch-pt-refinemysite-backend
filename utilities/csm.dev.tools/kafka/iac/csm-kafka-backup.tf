variable "kafka_backup_enabled" {
  type        = bool
  description = "Enable Backup Service"
  default     = true
}

resource "confluent_service_account" "kafka_backup" {
  count = var.kafka_backup_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-backup-${var.confluent_color_short}"
  description  = "${var.env} Backup Service ${var.confluent_color}"
}

resource "confluent_api_key" "kafka_backup_primary" {
  count = var.kafka_backup_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.kafka_backup[0].display_name
  description  = "${var.env} Backup Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.kafka_backup[0].id
    api_version = confluent_service_account.kafka_backup[0].api_version
    kind        = confluent_service_account.kafka_backup[0].kind
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

resource "confluent_api_key" "kafka_backup_secondary" {
  count = var.kafka_backup_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.kafka_backup[0].display_name
  description  = "${var.env} Backup Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.kafka_backup[0].id
    api_version = confluent_service_account.kafka_backup[0].api_version
    kind        = confluent_service_account.kafka_backup[0].kind
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

resource "azurerm_key_vault_secret" "kafka_backup_api_key_key" {
  count = var.kafka_backup_enabled ? 1 : 0

  name         = "csm-cloud-backup-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.kafka_backup_primary[0].id : confluent_api_key.kafka_backup_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "kafka_backup_api_key_secret" {
  count = var.kafka_backup_enabled ? 1 : 0

  name         = "csm-cloud-backup-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.kafka_backup_primary[0].secret : confluent_api_key.kafka_backup_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_acl" "kafka_backup_group_read" {
  count = var.kafka_backup_enabled && !var.is_backup_cluster ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "LITERAL"
  resource_name = "csm-backup-${var.env}"
  principal     = "User:${confluent_service_account.kafka_backup[0].id}"
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

resource "confluent_kafka_acl" "kafka_backup_topic_read" {
  count = var.kafka_backup_enabled && !var.is_backup_cluster ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.kafka_backup[0].id}"
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

resource "confluent_kafka_acl" "kafka_backup_topic_write" {
  # is backup cluster
  count = var.kafka_backup_enabled && var.is_backup_cluster ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "WRITE"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.kafka_backup[0].id}"
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

resource "confluent_kafka_acl" "kafka_backup_transactional_create" {
  # is backup cluster
  count = var.kafka_backup_enabled && var.is_backup_cluster ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TRANSACTIONAL_ID"
  operation     = "CREATE"
  pattern_type  = "PREFIXED"
  resource_name = "csm-backup-${var.env}"
  principal     = "User:${confluent_service_account.kafka_backup[0].id}"
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

resource "confluent_kafka_acl" "kafka_backup_transactional_write" {
  # is backup cluster
  count = var.kafka_backup_enabled && var.is_backup_cluster ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TRANSACTIONAL_ID"
  operation     = "WRITE"
  pattern_type  = "PREFIXED"
  resource_name = "csm-backup-${var.env}"
  principal     = "User:${confluent_service_account.kafka_backup[0].id}"
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

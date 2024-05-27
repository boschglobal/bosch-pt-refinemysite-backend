resource "confluent_service_account" "project_activity" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-pro-acy-${var.confluent_color_short}"
  description  = "${var.env} Project Activity Service ${var.confluent_color}"
}

resource "confluent_api_key" "project_activity_primary" {
  count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.project_activity[0].display_name
  description  = "${var.env} Project Activity Service ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.project_activity[0].id
    api_version = confluent_service_account.project_activity[0].api_version
    kind        = confluent_service_account.project_activity[0].kind
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

resource "confluent_api_key" "project_activity_secondary" {
  count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.project_activity[0].display_name
  description  = "${var.env} Project Activity Service ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.project_activity[0].id
    api_version = confluent_service_account.project_activity[0].api_version
    kind        = confluent_service_account.project_activity[0].kind
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

resource "azurerm_key_vault_secret" "project_activity_api_key_key" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-project-activity-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.project_activity_primary[0].id : confluent_api_key.project_activity_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "project_activity_api_key_secret" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-cloud-project-activity-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.project_activity_primary[0].secret : confluent_api_key.project_activity_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_acl" "project_activity_group_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "READ"
  pattern_type  = "LITERAL"
  resource_name = "csm-pm-activity-g2-${var.env}"
  principal     = "User:${confluent_service_account.project_activity[0].id}"
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

resource "confluent_kafka_acl" "project_activity_topic_read" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "READ"
  pattern_type  = "PREFIXED"
  resource_name = "csm.${var.env}"
  principal     = "User:${confluent_service_account.project_activity[0].id}"
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

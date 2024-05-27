resource "confluent_service_account" "kafka_lag_exporter" {
  count = var.services_enabled ? 1 : 0

  display_name = "csm-${local.env_short[var.env]}-met-kep-${var.confluent_color_short}"
  description  = "${var.env} Monitoring Metrics Kafka Lag Exporter ${var.confluent_color}"
}

resource "confluent_api_key" "kafka_lag_exporter_primary" {
  count = var.services_enabled && var.primary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.kafka_lag_exporter[0].display_name
  description  = "${var.env} Monitoring Metrics Kafka Lag Exporter ${var.confluent_color} primary"

  owner {
    id          = confluent_service_account.kafka_lag_exporter[0].id
    api_version = confluent_service_account.kafka_lag_exporter[0].api_version
    kind        = confluent_service_account.kafka_lag_exporter[0].kind
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

resource "confluent_api_key" "kafka_lag_exporter_secondary" {
  count = var.services_enabled && var.secondary_api_key_enabled ? 1 : 0

  display_name = confluent_service_account.kafka_lag_exporter[0].display_name
  description  = "${var.env} Monitoring Metrics Kafka Lag Exporter ${var.confluent_color} secondary"

  owner {
    id          = confluent_service_account.kafka_lag_exporter[0].id
    api_version = confluent_service_account.kafka_lag_exporter[0].api_version
    kind        = confluent_service_account.kafka_lag_exporter[0].kind
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

resource "azurerm_key_vault_secret" "kafka_lag_exporter_api_key_key" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-key-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.kafka_lag_exporter_primary[0].id : confluent_api_key.kafka_lag_exporter_secondary[0].id
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "azurerm_key_vault_secret" "kafka_lag_exporter_api_key_secret" {
  count = var.services_enabled ? 1 : 0

  name         = "csm-monitoring-metrics-kafka-lag-exporter-kafka-broker-api-secret-${var.confluent_color}"
  value        = var.active_api_key == "primary" ? confluent_api_key.kafka_lag_exporter_primary[0].secret : confluent_api_key.kafka_lag_exporter_secondary[0].secret
  key_vault_id = data.azurerm_key_vault.kafka_aks_kv.id

  tags = {
    file-encoding = "utf-8"
  }
}

resource "confluent_kafka_acl" "kafka_lag_exporter_group_describe" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "GROUP"
  operation     = "DESCRIBE"
  pattern_type  = "LITERAL"
  resource_name = "*"
  principal     = "User:${confluent_service_account.kafka_lag_exporter[0].id}"
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

resource "confluent_kafka_acl" "kafka_lag_exporter_topic_describe" {
  count = var.services_enabled ? 1 : 0

  permission    = "ALLOW"
  resource_type = "TOPIC"
  operation     = "DESCRIBE"
  pattern_type  = "LITERAL"
  resource_name = "*"
  principal     = "User:${confluent_service_account.kafka_lag_exporter[0].id}"
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

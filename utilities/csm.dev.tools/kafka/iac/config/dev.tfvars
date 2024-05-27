# Environment
env = "dev"

# Confluent
confluent_environment_name = "csm-dev"
confluent_cluster_name     = "csm-dev"
confluent_color            = "green"
confluent_color_short      = "gre"

# Cluster
cluster_type      = "Basic"
is_backup_cluster = false
services_enabled  = true

# Enable services
kafka_backup_enabled = false
reset_enabled        = true

# Enable Topic migration
topic_migration_enabled = false
topic_migration_name    = "csm.dev.projectmanagement.project.v2"

# Topics
bim_model_topic_partitions_count            = 2
company_topic_partitions_count              = 2
event_mobile_command_topic_partitions_count = 2
event_topic_partitions_count                = 2
featuretoggle_topic_partitions_count        = 1
image_scale_topic_partitions_count          = 1
job_command_topic_partitions_count          = 2
job_event_topic_partitions_count            = 8
project_delete_topic_partitions_count       = 1
project_invitation_topic_partitions_count   = 1
project_topic_partitions_count              = 2
storage_event_topic_partitions_count        = 1
user_consents_topic_partitions_count        = 1
user_craft_topic_partitions_count           = 1
user_pat_topic_partitions_count             = 2
user_topic_partitions_count                 = 2

company_topic_version_suffix = ".v2"

# Create, rotate and delete api keys
primary_api_key_enabled   = false
secondary_api_key_enabled = true
active_api_key            = "secondary" # use "primary" or "secondary" as values

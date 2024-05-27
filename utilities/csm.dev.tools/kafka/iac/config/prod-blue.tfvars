# Environment
env = "prod"

# Confluent
confluent_environment_name = "csm-production-blue"
confluent_cluster_name     = "csm-production-blue"
confluent_color            = "blue"
confluent_color_short      = "blu"

# Cluster
is_backup_cluster = false
services_enabled  = true

# Enable services
kafka_backup_enabled = true
reset_enabled        = false

# Enable Topic migration
topic_migration_enabled = false
topic_migration_name    = "csm.prod.projectmanagement.project.v7"

# Topics
bim_model_topic_partitions_count            = 6
company_topic_partitions_count              = 6
event_mobile_command_topic_partitions_count = 2
event_topic_partitions_count                = 6
featuretoggle_topic_partitions_count        = 1
image_scale_topic_partitions_count          = 1
job_command_topic_partitions_count          = 2
job_event_topic_partitions_count            = 16
project_delete_topic_partitions_count       = 3
project_invitation_topic_partitions_count   = 6
project_topic_partitions_count              = 18
storage_event_topic_partitions_count        = 1
user_consents_topic_partitions_count        = 1
user_craft_topic_partitions_count           = 1
user_pat_topic_partitions_count             = 2
user_topic_partitions_count                 = 6

company_topic_version_suffix = ".v2"
project_topic_version_suffix = ".v6"

# Create, rotate and delete api keys
primary_api_key_enabled   = false
secondary_api_key_enabled = true
active_api_key            = "secondary" # use "primary" or "secondary" as values

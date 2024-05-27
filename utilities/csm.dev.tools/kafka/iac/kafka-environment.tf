variable "confluent_environment_name" {
  type        = string
  description = "Name of the confluent environment"
}

resource "confluent_environment" "this" {
  display_name = var.confluent_environment_name

  lifecycle {
    prevent_destroy = true
  }
}

# DynamoDB table for URL entries
resource "aws_dynamodb_table" "url_entries" {
  name           = "${var.app_name}-url-entries-${local.environment}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 1
  write_capacity = 1
  table_class    = "STANDARD_IA"
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "DynamoDB table for storing URL entries"
  }

  lifecycle {
    ignore_changes = [tags]
  }
}

# DynamoDB table for click events
resource "aws_dynamodb_table" "click_events" {
  name           = "${var.app_name}-click-events-${local.environment}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 1
  write_capacity = 1
  table_class    = "STANDARD_IA"
  hash_key       = "shortUrlId"
  range_key      = "timestamp"

  attribute {
    name = "shortUrlId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "DynamoDB table for storing click events"
  }

  lifecycle {
    ignore_changes = [tags]
  }
}

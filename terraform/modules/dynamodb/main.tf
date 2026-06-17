###############################################################################
# DynamoDB tables for the URL shortener.
#
# Two tables:
#   - url-entries: PK = id (string), no sort key. Stores UrlEntry rows.
#   - click-events: PK = shortUrlId (string), SK = timestamp (number).
#                  Stores analytics rows. TTL is enabled on `ttl` so old rows
#                  are reclaimed automatically.
#
# Both tables use:
#   - PAY_PER_REQUEST billing so a portfolio demo doesn't get throttled at
#     1 RCU/WCU and doesn't pay per-hour for idle capacity.
#   - AWS-managed KMS encryption (SSE).
#   - Point-in-time recovery (35-day backup window) so a fat-fingered delete
#     can be reversed.
#   - Deletion protection so `terraform destroy` cannot silently drop data.
#
# click-events additionally has DynamoDB Streams enabled (NEW_AND_OLD_IMAGES)
# so future analytics consumers (Lambda → Kinesis Firehose → S3 / Athena) can
# react to clicks without polling.
###############################################################################

resource "aws_dynamodb_table" "url_entries" {
  name         = "${var.project_name}-url-entries-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"
  table_class  = "STANDARD"

  attribute {
    name = "id"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  deletion_protection_enabled = true

  tags = merge(var.tags, {
    Name        = "${var.project_name}-url-entries-${var.environment}"
    Environment = var.environment
    Module      = "dynamodb"
    Purpose     = "URL entries (id -> originalUrl)"
  })

  lifecycle {
    ignore_changes = [tags]
  }
}

resource "aws_dynamodb_table" "click_events" {
  name         = "${var.project_name}-click-events-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "shortUrlId"
  range_key    = "timestamp"
  table_class  = "STANDARD"

  attribute {
    name = "shortUrlId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  deletion_protection_enabled = true

  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  ttl {
    attribute_name = "ttl"
    enabled        = var.click_event_retention_days > 0
  }

  tags = merge(var.tags, {
    Name        = "${var.project_name}-click-events-${var.environment}"
    Environment = var.environment
    Module      = "dynamodb"
    Purpose     = "Click analytics (shortUrlId, timestamp) -> ip/ua/referrer/country"
  })

  lifecycle {
    ignore_changes = [tags]
  }
}

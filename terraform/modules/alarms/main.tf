###############################################################################
# CloudWatch alarms + SNS topic for the URL shortener stack.
#
# We create the SNS topic + email subscription ONCE per environment, then
# attach alarms to it. Subscription is email-based (a reviewer can swap
# to PagerDuty / Slack via a `protocol = "https"` override).
###############################################################################

resource "aws_sns_topic" "alarms" {
  name              = "${var.project_name}-${var.environment}-alarms"
  kms_master_key_id = "alias/aws/sns"

  tags = merge(var.tags, {
    Name        = "${var.project_name}-${var.environment}-alarms"
    Environment = var.environment
    Purpose     = "CloudWatch alarm fan-out"
  })
}

resource "aws_sns_topic_subscription" "email" {
  for_each  = toset(var.alarm_email_endpoints)
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = each.value
}

locals {
  lambda_dimensions = {
    FunctionName = var.lambda_function_name
  }
  api_dimensions = {
    ApiId = var.api_id
    Stage = var.api_stage_name
  }
  dynamo_url_dimensions = {
    TableName = var.url_entries_table_name
  }
  dynamo_click_dimensions = {
    TableName = var.click_events_table_name
  }
}

###############################################################################
# Lambda errors
###############################################################################

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${var.lambda_function_name}-errors"
  alarm_description   = "Lambda ${var.lambda_function_name} is erroring."
  namespace           = "AWS/Lambda"
  metric_name         = "Errors"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.lambda_error_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = local.lambda_dimensions
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "lambda_throttles" {
  alarm_name          = "${var.lambda_function_name}-throttles"
  alarm_description   = "Lambda ${var.lambda_function_name} is being throttled."
  namespace           = "AWS/Lambda"
  metric_name         = "Throttles"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = local.lambda_dimensions
  alarm_actions       = [aws_sns_topic.alarms.arn]

  tags = var.tags
}

###############################################################################
# API Gateway 5xx
###############################################################################

resource "aws_cloudwatch_metric_alarm" "api_5xx" {
  alarm_name          = "${var.project_name}-${var.environment}-api-5xx"
  alarm_description   = "API Gateway is returning 5xx for ${var.environment}."
  namespace           = "AWS/ApiGateway"
  metric_name         = "5xx"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.api_5xx_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = local.api_dimensions
  alarm_actions       = [aws_sns_topic.alarms.arn]

  tags = var.tags
}

###############################################################################
# DynamoDB throttling
###############################################################################

resource "aws_cloudwatch_metric_alarm" "dynamo_url_throttles" {
  alarm_name          = "${var.url_entries_table_name}-throttles"
  alarm_description   = "DynamoDB ${var.url_entries_table_name} throttling reads/writes."
  namespace           = "AWS/DynamoDB"
  metric_name         = "ThrottledRequests"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.dynamo_throttle_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = local.dynamo_url_dimensions
  alarm_actions       = [aws_sns_topic.alarms.arn]

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "dynamo_click_throttles" {
  alarm_name          = "${var.click_events_table_name}-throttles"
  alarm_description   = "DynamoDB ${var.click_events_table_name} throttling reads/writes."
  namespace           = "AWS/DynamoDB"
  metric_name         = "ThrottledRequests"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = var.dynamo_throttle_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = local.dynamo_click_dimensions
  alarm_actions       = [aws_sns_topic.alarms.arn]

  tags = var.tags
}

# CloudWatch dashboards
resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = "${var.project_name}-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", var.lambda_function_name],
            [".", "Errors", ".", "."],
            [".", "Throttles", ".", "."],
            [".", "Duration", ".", "."],
          ]
          view    = "timeSeries"
          stacked = false
          region  = data.aws_region.current.name
          title   = "Lambda"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApiGateway", "Count", "ApiId", var.api_id, "Stage", var.api_stage_name],
            [".", "4xx", ".", ".", "."],
            [".", "5xx", ".", ".", "."],
            [".", "Latency", ".", ".", "."],
          ]
          view    = "timeSeries"
          stacked = false
          region  = data.aws_region.current.name
          title   = "API Gateway"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 24
        height = 6
        properties = {
          metrics = [
            ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", var.url_entries_table_name],
            [".", "ConsumedWriteCapacityUnits", ".", "."],
            [".", "ThrottledRequests", ".", "."],
            [".", "ConsumedReadCapacityUnits", ".", var.click_events_table_name],
            [".", "ConsumedWriteCapacityUnits", ".", "."],
            [".", "ThrottledRequests", ".", "."],
          ]
          view    = "timeSeries"
          stacked = false
          region  = data.aws_region.current.name
          title   = "DynamoDB"
          period  = 300
        }
      },
    ]
  })
}

data "aws_region" "current" {}

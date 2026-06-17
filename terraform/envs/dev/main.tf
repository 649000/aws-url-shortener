###############################################################################
# Dev environment entry point.
#
# Wires the modules together for the `dev` workspace. State is held in S3 at
# the path declared in backend.hcl (overridden at `terraform init` time).
###############################################################################

terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = "649000/aws-url-shortener"
    }
  }
}

locals {
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

###############################################################################
# Data layer
###############################################################################

module "dynamodb" {
  source = "../../modules/dynamodb"

  project_name               = var.project_name
  environment                = var.environment
  tags                       = local.tags
  click_event_retention_days = 30
}

###############################################################################
# Compute
#
# iam_statements: grant the Lambda exactly the permissions it needs against
# the two tables. Inline so future changes are easy to spot in code review.
###############################################################################

module "lambda" {
  source = "../../modules/lambda"

  project_name       = var.project_name
  environment        = var.environment
  tags               = local.tags
  deployment_type    = var.deployment_type
  lambda_zip_path    = "${path.module}/../../../target/function.zip"
  log_retention_days = 7

  environment_variables = {
    QUARKUS_DYNAMODB_AWS_REGION           = var.aws_region
    QUARKUS_DYNAMODB_AWS_CREDENTIALS_TYPE = "default"
    APP_DYNAMODB_URL_TABLE                = module.dynamodb.url_entries_table_name
    APP_DYNAMODB_CLICK_EVENT_TABLE        = module.dynamodb.click_events_table_name
  }

  iam_statements = [
    {
      sid    = "DynamoUrlEntries"
      effect = "Allow"
      actions = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:Query",
        "dynamodb:Scan",
      ]
      resources = [
        module.dynamodb.url_entries_table_arn,
        "${module.dynamodb.url_entries_table_arn}/index/*",
      ]
    },
    {
      sid    = "DynamoClickEvents"
      effect = "Allow"
      actions = [
        "dynamodb:PutItem",
        "dynamodb:Query",
        "dynamodb:Scan",
      ]
      resources = [
        module.dynamodb.click_events_table_arn,
        "${module.dynamodb.click_events_table_arn}/index/*",
      ]
    },
    {
      sid       = "DynamoStreamsClickEvents"
      effect    = "Allow"
      actions   = ["dynamodb:DescribeStream", "dynamodb:GetRecords", "dynamodb:GetShardIterator", "dynamodb:ListStreams"]
      resources = [module.dynamodb.click_events_table_stream_arn]
    },
    {
      sid       = "SNSPublishDLQ"
      effect    = "Allow"
      actions   = ["sns:Publish"]
      resources = ["arn:aws:sns:${var.aws_region}:*:${var.project_name}-${var.environment}*"]
    },
  ]
}

###############################################################################
# Edge
###############################################################################

module "api_gateway" {
  source = "../../modules/api-gateway"

  project_name         = var.project_name
  environment          = var.environment
  tags                 = local.tags
  lambda_invoke_arn    = module.lambda.invoke_arn
  throttle_burst_limit = 200
  throttle_rate_limit  = 100
}

module "cloudfront" {
  source = "../../modules/cloudfront"
  count  = var.enable_cloudfront ? 1 : 0

  project_name       = var.project_name
  environment        = var.environment
  tags               = local.tags
  api_endpoint       = module.api_gateway.api_endpoint
  custom_domain_name = var.cloudfront_domain_name
  certificate_arn    = var.cloudfront_certificate_arn
  price_class        = "PriceClass_100"
}

###############################################################################
# Observability
###############################################################################

module "alarms" {
  source = "../../modules/alarms"

  project_name            = var.project_name
  environment             = var.environment
  tags                    = local.tags
  lambda_function_name    = module.lambda.function_name
  api_id                  = module.api_gateway.api_id
  api_stage_name          = module.api_gateway.stage_name
  url_entries_table_name  = module.dynamodb.url_entries_table_name
  click_events_table_name = module.dynamodb.click_events_table_name
  alarm_email_endpoints   = var.alarm_email_endpoints
}

###############################################################################
# Outputs
###############################################################################

output "api_invoke_url" {
  description = "Public URL of the API."
  value       = module.api_gateway.invoke_url
}

output "cloudfront_domain" {
  description = "CloudFront domain (if enabled)."
  value       = var.enable_cloudfront ? module.cloudfront[0].distribution_domain_name : ""
}

output "lambda_function_name" {
  value = module.lambda.function_name
}

output "dynamodb_tables" {
  value = {
    url_entries  = module.dynamodb.url_entries_table_name
    click_events = module.dynamodb.click_events_table_name
  }
}

output "alarms_topic_arn" {
  value = module.alarms.alarms_topic_arn
}

output "dashboard_name" {
  value = module.alarms.dashboard_name
}

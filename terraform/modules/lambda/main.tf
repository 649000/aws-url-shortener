###############################################################################
# Quarkus-on-Lambda packaging. The build artifact is a plain .zip uploaded
# directly to Lambda; the Quarkus Maven plugin produces a self-contained
# "quarkus-run.jar" via `mvn package`, which we zip into target/function.zip.
#
# Two runtimes:
#   - non-native → java21 + QuarkusStreamHandler
#   - native     → provided.al2 (Amazon Linux 2 custom runtime) + binary named `function`
#
# Quality-of-life add-ons:
#   - X-Ray active tracing for distributed-debugging signal in production.
#   - SNS dead-letter topic so async failures don't disappear silently.
#   - CloudWatch log group with explicit retention (no `Never expire`).
###############################################################################

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  function_name = "${var.project_name}-${var.environment}"
  # Quarkus's Maven layout puts the runnable at target/quarkus-app/quarkus-run.jar.
  # We accept an explicit path so this module can be reused regardless of where
  # the deploy pipeline drops the zip.
  is_native = var.deployment_type == "native"
  handler   = local.is_native ? "function" : "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtimes  = local.is_native ? "provided.al2" : "java21"
}

resource "aws_sns_topic" "lambda_dlq" {
  name              = "${local.function_name}-dlq"
  kms_master_key_id = "alias/aws/sns"

  tags = merge(var.tags, {
    Name        = "${local.function_name}-dlq"
    Environment = var.environment
    Purpose     = "Lambda dead-letter queue target"
  })
}

resource "aws_sns_topic_policy" "lambda_dlq" {
  arn = aws_sns_topic.lambda_dlq.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowLambdaPublish"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action   = "SNS:Publish"
        Resource = aws_sns_topic.lambda_dlq.arn
        Condition = {
          StringEquals = {
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${local.function_name}"
  retention_in_days = var.log_retention_days
  kms_key_id        = "alias/aws/logs"

  tags = merge(var.tags, {
    Name        = "/aws/lambda/${local.function_name}"
    Environment = var.environment
  })
}

resource "aws_lambda_function" "app" {
  function_name = local.function_name
  role          = aws_iam_role.lambda_execution.arn
  filename      = var.lambda_zip_path
  handler       = local.handler
  runtime       = local.runtimes
  timeout       = var.timeout
  memory_size   = var.memory_size

  # Always set source_code_hash so a no-op `terraform apply` that picked up
  # a stale zip still forces Lambda to re-deploy. filebase64sha256 is the
  # AWS-recommended hashing strategy for zip artifacts.
  source_code_hash = filebase64sha256(var.lambda_zip_path)

  reserved_concurrent_executions = var.reserved_concurrent_executions

  tracing_config {
    mode = "Active"
  }

  dead_letter_config {
    target_arn = aws_sns_topic.lambda_dlq.arn
  }

  environment {
    variables = merge(
      {
        DEPLOYMENT_TYPE = var.deployment_type
      },
      var.environment_variables,
    )
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda,
    aws_sns_topic_policy.lambda_dlq,
  ]

  tags = merge(var.tags, {
    Name        = local.function_name
    Environment = var.environment
    Purpose     = "Quarkus URL shortener"
  })

  lifecycle {
    ignore_changes = [
      # The zip hash is set explicitly above; ignore tag churn.
      tags,
    ]
  }
}

###############################################################################
# IAM
###############################################################################

data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lambda_execution" {
  name               = "${local.function_name}-execution-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json

  tags = merge(var.tags, {
    Name        = "${local.function_name}-execution-role"
    Environment = var.environment
  })
}

data "aws_iam_policy_document" "lambda_policy" {
  # CloudWatch Logs (scoped to this function's log group, not "*").
  statement {
    sid    = "WriteLogs"
    effect = "Allow"
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = [
      "${aws_cloudwatch_log_group.lambda.arn}:*",
    ]
  }

  # X-Ray.
  statement {
    sid       = "XRay"
    effect    = "Allow"
    actions   = ["xray:PutTraceSegments", "xray:PutTelemetryRecords"]
    resources = ["*"]
  }

  # Caller passes additional statements via var.iam_statements so each
  # deployment can grant DynamoDB / SNS / etc. without modifying this module.
  dynamic "statement" {
    for_each = var.iam_statements
    content {
      sid       = statement.value.sid
      effect    = statement.value.effect
      actions   = statement.value.actions
      resources = statement.value.resources
    }
  }
}

resource "aws_iam_role_policy" "lambda_policy" {
  name   = "${local.function_name}-execution-policy"
  role   = aws_iam_role.lambda_execution.id
  policy = data.aws_iam_policy_document.lambda_policy.json

  # The dynamic block above references these; default to [] in variables.tf.
}

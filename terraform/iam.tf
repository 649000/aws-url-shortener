# IAM role for Lambda execution
resource "aws_iam_role" "lambda_execution" {
  name = "${var.app_name}-lambda-execution-${local.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "IAM role for Lambda execution"
  }
}

# IAM policy for Lambda execution
resource "aws_iam_role_policy" "lambda_policy" {
  name = "${var.app_name}-lambda-policy-${local.environment}"
  role = aws_iam_role.lambda_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Effect   = "Allow"
        Resource = [
          aws_dynamodb_table.url_entries.arn,
          aws_dynamodb_table.click_events.arn
        ]
      },
      {
        Action = "logs:CreateLogGroup"
        Effect = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect   = "Allow"
        Resource = "${aws_cloudwatch_log_group.lambda.arn}:*"
      }
    ]
  })

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "IAM policy for Lambda execution"
  }
}

# CloudWatch log group for Lambda
resource "aws_cloudwatch_log_group" "lambda" {
  name = "/aws/lambda/${var.app_name}-${local.environment}"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "CloudWatch log group for Lambda functions"
  }
}

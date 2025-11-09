# Lambda function for Quarkus application
resource "aws_lambda_function" "app" {
  filename         = "target/function.zip" # Use the provided zip file for the Lambda function
  function_name    = "${var.app_name}-${local.environment}"
  role             = aws_iam_role.lambda_execution.arn
  handler          = var.deployment_type == "native" ? "function" : "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime          = var.deployment_type == "native" ? "provided.al2" : "java21"
  timeout          = 30
  memory_size      = 1024

  environment {
    variables = {
      QUARKUS_DYNAMODB_AWS_REGION = "ap-southeast-1"
      QUARKUS_DYNAMODB_AWS_CREDENTIALS_TYPE = "default"
      DEPLOYMENT_TYPE = var.deployment_type
    }
  }

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "Lambda function for Quarkus application"
  }

  dynamic "source_code_hash" {
    for_each = var.deployment_type == "native" ? [1] : []
    content {
      source_code_hash = filebase64sha256("target/function.zip")
    }
  }

  dynamic "source_code_hash" {
    for_each = var.deployment_type == "non-native" ? [1] : []
    content {
      source_code_hash = filebase64sha256("target/function.zip")
    }
  }
}

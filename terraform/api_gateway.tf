# HTTP API Gateway for Quarkus application
resource "aws_apigatewayv2_api" "http_api" {
  name          = "${var.app_name}-http-api-${local.environment}"
  protocol_type = "HTTP"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway for Quarkus application"
  }
}

# HTTP API Gateway integration with Lambda
resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id             = aws_apigatewayv2_api.http_api.id
  integration_type   = "AWS_PROXY"
  integration_uri    = aws_lambda_function.app.invoke_arn
  payload_format_version = "2.0"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway integration with Lambda function"
  }
}

# HTTP API Gateway route for POST /shorten
resource "aws_apigatewayv2_route" "shorten_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /shorten"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway route for POST /shorten"
  }
}

# HTTP API Gateway route for GET /{id}
resource "aws_apigatewayv2_route" "redirect_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /{id}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway route for GET /{id}"
  }
}

# HTTP API Gateway catch-all route
resource "aws_apigatewayv2_route" "catch_all_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway catch-all route for any additional routes"
  }
}

# HTTP API Gateway deployment
resource "aws_apigatewayv2_deployment" "deployment" {
  api_id = aws_apigatewayv2_api.http_api.id

  # Trigger redeployment when the integration changes
  triggers = {
    redeployment = sha1(jsonencode([
      aws_apigatewayv2_integration.lambda_integration.id
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway deployment"
  }
}

# HTTP API Gateway stage
resource "aws_apigatewayv2_stage" "stage" {
  api_id = aws_apigatewayv2_api.http_api.id
  name   = local.environment

  deployment_id = aws_apigatewayv2_deployment.deployment.id

  tags = {
    environment = local.environment
    project     = var.app_name
    Description = "HTTP API Gateway stage"
  }
}

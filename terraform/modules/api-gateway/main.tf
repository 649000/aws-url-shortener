###############################################################################
# HTTP API Gateway v2 in front of the Quarkus Lambda.
#
# Routes are configurable so this module can serve both the URL shortener and
# any future microservice. The catch-all route ({proxy+}) is preserved as a
# safety net for JAX-RS resources the gateway hasn't been told about explicitly.
#
# Quality-of-life add-ons:
#   - Throttling (configurable burst + steady-state rate) per stage.
#   - Access logging to a dedicated CloudWatch log group with explicit retention.
#   - CORS preflight handled by the gateway itself (OPTIONS /{proxy+}).
#   - Deployment triggers hash BOTH the route set and the integration, so
#     adding/removing a route forces a redeploy.
###############################################################################

resource "aws_apigatewayv2_api" "http_api" {
  name          = "${var.project_name}-http-api-${var.environment}"
  protocol_type = "HTTP"
  description   = "HTTP API for ${var.project_name} (${var.environment})"

  # CORS is configured at the API level so every route inherits it.
  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "OPTIONS"]
    allow_headers = ["*"]
    max_age       = 300
  }

  tags = merge(var.tags, {
    Name        = "${var.project_name}-http-api-${var.environment}"
    Environment = var.environment
  })
}

resource "aws_apigatewayv2_integration" "lambda_integration" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.lambda_invoke_arn
  payload_format_version = "2.0"

  # Make sure async failures surface as 5xx so throttling metrics are accurate.
  timeout_milliseconds = 30000
}

###############################################################################
# Routes: one per var.route_keys entry. We deliberately create them as
# separate resources (not via `for_each`) so each route gets its own
# human-friendly addressable ID and so `terraform state mv` works.
###############################################################################

resource "aws_apigatewayv2_route" "this" {
  for_each = toset(var.route_keys)

  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

# Catch-all proxy route. Lower priority than the explicit routes above.
resource "aws_apigatewayv2_route" "catch_all" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_integration.id}"
}

###############################################################################
# Access logging. Dedicated log group so retention is independent of the
# Lambda log group, and so dashboards/alarms can filter on it.
###############################################################################

resource "aws_cloudwatch_log_group" "api_access" {
  name              = "/aws/apigateway/${var.project_name}-${var.environment}-access"
  retention_in_days = var.access_log_retention_days
  kms_key_id        = "alias/aws/logs"

  tags = merge(var.tags, {
    Name        = "/aws/apigateway/${var.project_name}-${var.environment}-access"
    Environment = var.environment
  })
}

###############################################################################
# Deployment + Stage. The trigger hash includes BOTH routes and integration
# so any meaningful config change forces a clean redeploy.
###############################################################################

resource "aws_apigatewayv2_deployment" "this" {
  api_id = aws_apigatewayv2_api.http_api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_apigatewayv2_integration.lambda_integration.id,
      [for r in aws_apigatewayv2_route.this : r.id],
      aws_apigatewayv2_route.catch_all.id,
      var.route_keys,
      var.throttle_burst_limit,
      var.throttle_rate_limit,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_apigatewayv2_stage" "this" {
  api_id        = aws_apigatewayv2_api.http_api.id
  name          = var.environment
  description   = "${var.project_name} ${var.environment} stage"
  deployment_id = aws_apigatewayv2_deployment.this.id

  # Per-route defaults for throttling. API Gateway v2 applies these to any
  # route that doesn't override them.
  default_route_settings {
    throttling_burst_limit = var.throttle_burst_limit
    throttling_rate_limit  = var.throttle_rate_limit
  }

  # Access logs in CLF-ish format that's easy to ship to Athena / ES later.
  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_access.arn
    format = jsonencode({
      requestId        = "$context.requestId"
      ip               = "$context.identity.sourceIp"
      requestTime      = "$context.requestTime"
      httpMethod       = "$context.httpMethod"
      path             = "$context.path"
      status           = "$context.status"
      protocol         = "$context.protocol"
      responseLength   = "$context.responseLength"
      integrationError = "$context.integrationErrorMessage"
      errorMessage     = "$context.error.message"
    })
  }

  tags = merge(var.tags, {
    Environment = var.environment
  })
}

###############################################################################
# Optional custom domain. Disabled when both vars are empty so a vanilla
# `terraform apply` works without ACM setup.
###############################################################################

resource "aws_apigatewayv2_domain_name" "this" {
  count = var.custom_domain_name != "" ? 1 : 0

  domain_name = var.custom_domain_name

  domain_name_configuration {
    certificate_arn = var.certificate_arn
    endpoint_type   = "REGIONAL"
    security_policy = "TLS_1_2"
  }

  tags = merge(var.tags, {
    Environment = var.environment
  })
}

resource "aws_apigatewayv2_api_mapping" "this" {
  count = var.custom_domain_name != "" ? 1 : 0

  api_id      = aws_apigatewayv2_api.http_api.id
  domain_name = aws_apigatewayv2_domain_name.this[0].domain_name
  stage       = aws_apigatewayv2_stage.this.name
}

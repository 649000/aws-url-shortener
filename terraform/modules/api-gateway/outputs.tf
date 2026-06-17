output "api_id" {
  description = "API Gateway HTTP API ID."
  value       = aws_apigatewayv2_api.http_api.id
}

output "api_endpoint" {
  description = "Default API Gateway invoke URL (no stage)."
  value       = aws_apigatewayv2_api.http_api.api_endpoint
}

output "invoke_url" {
  description = "Stage-qualified invoke URL. This is the URL a public client would use."
  value       = "${aws_apigatewayv2_api.http_api.api_endpoint}/${aws_apigatewayv2_stage.this.name}"
}

output "stage_name" {
  description = "Stage name (typically matches `var.environment`)."
  value       = aws_apigatewayv2_stage.this.name
}

output "custom_domain_name" {
  description = "Custom domain bound to the API, or empty string when no custom domain is configured."
  value       = try(aws_apigatewayv2_domain_name.this[0].domain_name, "")
}

output "access_log_group_name" {
  description = "CloudWatch Logs group containing the access logs."
  value       = aws_cloudwatch_log_group.api_access.name
}

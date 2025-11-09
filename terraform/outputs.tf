# Output the API Gateway URL
output "api_gateway_url" {
  description = "The URL of the HTTP API Gateway"
  value       = "${aws_apigatewayv2_api.http_api.api_endpoint}/${local.environment}"
}

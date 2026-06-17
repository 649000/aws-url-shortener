variable "project_name" {
  description = "Short project name used as a prefix for all resource names."
  type        = string
}

variable "environment" {
  description = "Deployment environment (e.g. dev, prod)."
  type        = string
}

variable "tags" {
  description = "Additional tags merged into every resource created by this module."
  type        = map(string)
  default     = {}
}

variable "lambda_invoke_arn" {
  description = "Lambda invoke ARN (from the lambda module's `invoke_arn` output)."
  type        = string
}

variable "custom_domain_name" {
  description = "Optional custom domain to bind the API to (e.g. api.example.com)."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ACM certificate ARN for the custom domain (must be in us-east-1 for CloudFront-fronted setups, or in-region otherwise)."
  type        = string
  default     = ""
}

variable "route_keys" {
  description = "List of route keys (e.g. ['POST /shorten', 'GET /{id}']) the API Gateway exposes. Defaults to the canonical URL-shortener routes."
  type        = list(string)
  default = [
    "POST /shorten",
    "GET /{id}",
    "GET /{id}/analytics/clicks",
    "GET /{id}/analytics/unique-clicks",
    "GET /{id}/analytics/clicks-by-country",
    "GET /{id}/analytics/clicks-by-referrer",
    "GET /{id}/analytics/clicks-by-user-agent",
    "GET /{id}/analytics/clicks-in-time-range",
  ]
}

variable "throttle_burst_limit" {
  description = "API Gateway burst limit (per client). Default 100."
  type        = number
  default     = 100
}

variable "throttle_rate_limit" {
  description = "API Gateway steady-state rate limit (requests/sec, per client). Default 50."
  type        = number
  default     = 50
}

variable "access_log_retention_days" {
  description = "Retention in days for the API Gateway access log group."
  type        = number
  default     = 30
}

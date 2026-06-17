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

variable "api_endpoint" {
  description = "Default API Gateway invoke URL (from the api-gateway module's `api_endpoint` output)."
  type        = string
}

variable "price_class" {
  description = "CloudFront price class. PriceClass_100 = NA+EU only, cheapest."
  type        = string
  default     = "PriceClass_100"
}

variable "custom_domain_name" {
  description = "Optional custom domain to bind the distribution to (e.g. go.example.com)."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ACM certificate ARN in us-east-1 for the custom domain. Required when custom_domain_name is set."
  type        = string
  default     = ""
}

variable "waf_acl_arn" {
  description = "Optional AWS WAFv2 Web ACL ARN to attach. Set to empty string to disable WAF."
  type        = string
  default     = ""
}

variable "access_log_retention_days" {
  description = "Retention for the CloudFront access log bucket."
  type        = number
  default     = 30
}

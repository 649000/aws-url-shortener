variable "project_name" {
  description = "Short project name used as a prefix for all resource names."
  type        = string
  default     = "aws-url-shortener"
}

variable "environment" {
  description = "Deployment environment (matches the Terraform workspace name)."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "Primary AWS region for this environment."
  type        = string
  default     = "ap-southeast-1"
}

variable "deployment_type" {
  description = "Either 'native' (GraalVM native image) or 'non-native' (JVM)."
  type        = string
  default     = "non-native"
}

variable "enable_cloudfront" {
  description = "Provision a CloudFront distribution in front of the API."
  type        = bool
  default     = false
}

variable "cloudfront_domain_name" {
  description = "Optional custom domain to bind to CloudFront (e.g. dev.go.example.com)."
  type        = string
  default     = ""
}

variable "cloudfront_certificate_arn" {
  description = "ACM certificate ARN (in us-east-1) for the custom CloudFront domain."
  type        = string
  default     = ""
}

variable "alarm_email_endpoints" {
  description = "Email addresses that should receive CloudWatch alarm notifications."
  type        = list(string)
  default     = []
}

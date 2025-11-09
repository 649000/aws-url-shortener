# Application service name
variable "app_name" {
  description = "Name of the application/service"
  type        = string
  default     = "aws-url-shortener"
}

# Deployment environment identifier
variable "environment" {
  description = "Deployment environment (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"
}

# Deployment type (native or non-native)
variable "deployment_type" {
  description = "Deployment type for Lambda function (native or non-native)"
  type        = string
  default     = "non-native"
  validation {
    condition     = contains(["native", "non-native"], var.deployment_type)
    error_message = "Deployment type must be either 'native' or 'non-native'."
  }
}

# Get current workspace
locals {
  environment = terraform.workspace == "default" ? var.environment : terraform.workspace
}

variable "enable_cloudfront" {
  description = "Enable CloudFront distribution for the URL shortener"
  type        = bool
  default     = false
}

variable "cloudfront_domain_name" {
  description = "Custom domain name for CloudFront distribution (optional)"
  type        = string
  default     = ""
}

variable "cloudfront_certificate_arn" {
  description = "ARN of the ACM certificate for CloudFront (required if using custom domain)"
  type        = string
  default     = ""
}

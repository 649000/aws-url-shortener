# Application service name
variable "app_name" {
  description = "The name of the application/service"
  type        = string
  default     = "aws-url-shortener"
}

# Deployment environment identifier
variable "environment" {
  description = "The deployment environment (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"
}
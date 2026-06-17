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

variable "deployment_type" {
  description = "Either 'native' (GraalVM native image) or 'non-native' (JVM)."
  type        = string
  default     = "non-native"

  validation {
    condition     = contains(["native", "non-native"], var.deployment_type)
    error_message = "deployment_type must be 'native' or 'non-native'."
  }
}

variable "lambda_zip_path" {
  description = "Path (relative to the Terraform working dir) of the zip artifact uploaded to Lambda."
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention in days for the Lambda log group."
  type        = number
  default     = 30
}

variable "memory_size" {
  description = "Lambda memory size in MB."
  type        = number
  default     = 1024
}

variable "timeout" {
  description = "Lambda execution timeout in seconds."
  type        = number
  default     = 30
}

variable "reserved_concurrent_executions" {
  description = "Reserved concurrent executions for the Lambda. -1 means unreserved."
  type        = number
  default     = -1
}

variable "environment_variables" {
  description = "Map of environment variables to expose to the Lambda runtime."
  type        = map(string)
  default     = {}
}

variable "iam_statements" {
  description = <<-EOT
    Additional IAM statements to attach to the Lambda execution role.
    Each statement is an object with sid, effect, actions, and resources.
    Example:
      {
        sid       = "DynamoReadWrite"
        effect    = "Allow"
        actions   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:Query"]
        resources = ["arn:aws:dynamodb:*:*:table/foo"]
      }
  EOT
  type = list(object({
    sid       = string
    effect    = string
    actions   = list(string)
    resources = list(string)
  }))
  default = []
}

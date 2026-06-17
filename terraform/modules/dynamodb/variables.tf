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

variable "click_event_retention_days" {
  description = "Days to retain click-event rows before DynamoDB expires them. Set to 0 to disable TTL."
  type        = number
  default     = 90
}

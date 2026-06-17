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

variable "lambda_function_name" {
  description = "Name of the Lambda function to monitor."
  type        = string
}

variable "api_id" {
  description = "API Gateway HTTP API ID."
  type        = string
}

variable "api_stage_name" {
  description = "API Gateway stage name."
  type        = string
}

variable "url_entries_table_name" {
  description = "DynamoDB URL entries table name."
  type        = string
}

variable "click_events_table_name" {
  description = "DynamoDB click events table name."
  type        = string
}

variable "lambda_error_threshold" {
  description = "Number of Lambda errors in 5 minutes that trip the alarm."
  type        = number
  default     = 5
}

variable "api_5xx_threshold" {
  description = "Number of API Gateway 5xx responses in 5 minutes that trip the alarm."
  type        = number
  default     = 10
}

variable "dynamo_throttle_threshold" {
  description = "DynamoDB throttle events in 5 minutes that trip the alarm."
  type        = number
  default     = 5
}

variable "alarm_email_endpoints" {
  description = "Comma-separated list of email addresses to subscribe to the alarms topic."
  type        = list(string)
  default     = []
}

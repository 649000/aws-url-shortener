output "alarms_topic_arn" {
  description = "ARN of the SNS topic that receives all alarm notifications."
  value       = aws_sns_topic.alarms.arn
}

output "dashboard_name" {
  description = "CloudWatch dashboard name."
  value       = aws_cloudwatch_dashboard.this.dashboard_name
}

output "function_name" {
  description = "Name of the deployed Lambda function."
  value       = aws_lambda_function.app.function_name
}

output "function_arn" {
  description = "ARN of the deployed Lambda function."
  value       = aws_lambda_function.app.arn
}

output "invoke_arn" {
  description = "Invocation ARN (used by API Gateway AWS_PROXY integrations)."
  value       = aws_lambda_function.app.invoke_arn
}

output "role_arn" {
  description = "ARN of the Lambda execution IAM role."
  value       = aws_iam_role.lambda_execution.arn
}

output "log_group_name" {
  description = "CloudWatch Logs group for the Lambda function."
  value       = aws_cloudwatch_log_group.lambda.name
}

output "dlq_topic_arn" {
  description = "ARN of the SNS topic that receives async Lambda failures."
  value       = aws_sns_topic.lambda_dlq.arn
}

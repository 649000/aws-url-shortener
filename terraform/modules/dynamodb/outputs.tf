output "url_entries_table_name" {
  description = "Name of the DynamoDB table that stores URL entries."
  value       = aws_dynamodb_table.url_entries.name
}

output "url_entries_table_arn" {
  description = "ARN of the DynamoDB table that stores URL entries."
  value       = aws_dynamodb_table.url_entries.arn
}

output "url_entries_table_stream_arn" {
  description = "Stream ARN for the URL entries table (empty when streams are disabled)."
  value       = aws_dynamodb_table.url_entries.stream_arn
}

output "click_events_table_name" {
  description = "Name of the DynamoDB table that stores click events."
  value       = aws_dynamodb_table.click_events.name
}

output "click_events_table_arn" {
  description = "ARN of the DynamoDB table that stores click events."
  value       = aws_dynamodb_table.click_events.arn
}

output "click_events_table_stream_arn" {
  description = "Stream ARN for the click events table. Subscribe a Lambda here for analytics pipelines."
  value       = aws_dynamodb_table.click_events.stream_arn
}

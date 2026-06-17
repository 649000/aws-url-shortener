output "distribution_id" {
  description = "CloudFront distribution ID."
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_domain_name" {
  description = "CloudFront distribution domain (e.g. d123.cloudfront.net)."
  value       = aws_cloudfront_distribution.this.domain_name
}

output "distribution_arn" {
  description = "CloudFront distribution ARN."
  value       = aws_cloudfront_distribution.this.arn
}

output "access_log_bucket" {
  description = "S3 bucket that holds CloudFront access logs."
  value       = aws_s3_bucket.access_logs.id
}

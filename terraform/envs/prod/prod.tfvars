# Override defaults for the prod environment.
# Apply with: terraform apply -var-file=prod.tfvars

project_name    = "aws-url-shortener"
environment     = "prod"
aws_region      = "ap-southeast-1"
deployment_type = "non-native"

# CloudFront on by default in prod. Set custom domain + ACM cert for go.example.com.
enable_cloudfront          = true
cloudfront_domain_name     = ""
cloudfront_certificate_arn = ""

# Optional WAFv2 ACL ARN. Leave blank to attach none.
waf_acl_arn = ""

# PagerDuty / on-call distribution lists recommended.
alarm_email_endpoints = []

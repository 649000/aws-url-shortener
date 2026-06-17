# Override defaults for the dev environment.
# Apply with: terraform apply -var-file=dev.tfvars

project_name               = "aws-url-shortener"
environment                = "dev"
aws_region                 = "ap-southeast-1"
deployment_type            = "non-native"
enable_cloudfront          = false
cloudfront_domain_name     = ""
cloudfront_certificate_arn = ""

# Add your email to receive dev alarm notifications:
# alarm_email_endpoints = ["you@example.com"]
alarm_email_endpoints = []

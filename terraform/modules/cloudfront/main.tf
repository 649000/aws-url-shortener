###############################################################################
# CloudFront in front of the API Gateway.
#
# Why CloudFront?
#   - Edge caching for redirect responses (GET /{id} is read-mostly).
#   - Single regional endpoint with a global anycast URL.
#   - Optional WAF association (var.waf_acl_arn) for rate-limit + bot rules.
#
# Access logs land in a dedicated S3 bucket with lifecycle expiry so they
# don't accumulate forever.
###############################################################################

resource "aws_s3_bucket" "access_logs" {
  bucket        = "${var.project_name}-${var.environment}-cf-logs"
  force_destroy = false

  tags = merge(var.tags, {
    Name        = "${var.project_name}-${var.environment}-cf-logs"
    Environment = var.environment
    Purpose     = "CloudFront access logs"
  })
}

resource "aws_s3_bucket_lifecycle_configuration" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  rule {
    id     = "expire-old-logs"
    status = "Enabled"

    # Apply to the whole bucket; explicit empty filter is required by the AWS
    # provider 5.x schema (otherwise you get a deprecation warning).
    filter {}

    expiration {
      days = var.access_log_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 7
    }
  }
}

resource "aws_s3_bucket_public_access_block" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront logs need to write to the bucket. Server-side encryption + log
# delivery policy is documented in the AWS docs.
data "aws_iam_policy_document" "access_logs_bucket" {
  statement {
    sid     = "AllowCloudFrontLogDelivery"
    effect  = "Allow"
    actions = ["s3:PutObject"]
    resources = [
      "${aws_s3_bucket.access_logs.arn}/*",
    ]
    principals {
      type        = "Service"
      identifiers = ["delivery.logs.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "s3:x-amz-acl"
      values   = ["bucket-owner-full-control"]
    }
  }

  statement {
    sid     = "AllowCloudFrontLogDeliveryGetBucketAcl"
    effect  = "Allow"
    actions = ["s3:GetBucketAcl"]
    resources = [
      aws_s3_bucket.access_logs.arn,
    ]
    principals {
      type        = "Service"
      identifiers = ["delivery.logs.amazonaws.com"]
    }
  }
}

resource "aws_s3_bucket_policy" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id
  policy = data.aws_iam_policy_document.access_logs_bucket.json
}

locals {
  domain_aliases = var.custom_domain_name != "" ? [var.custom_domain_name] : []
  acm_arn        = var.certificate_arn != "" ? var.certificate_arn : null
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  comment         = "${var.project_name} ${var.environment}"
  price_class     = var.price_class

  origin {
    domain_name = replace(var.api_endpoint, "https://", "")
    origin_id   = "apigateway"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id = "apigateway"

    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods  = ["GET", "HEAD"]

    # Cache redirects (3xx) and analytics GETs for 60s; everything else
    # bypasses cache. Adjust in the api-gateway module's stage if needed.
    cache_policy_id          = "413cee28-aac1-4098-90b4-7a0e35301979" # CachingDisabled
    origin_request_policy_id = "b689b0a8-53d0-40ab-baf2-08ae9e2b5dc5" # AllViewer

    # Defensive: 3xx responses can be served from CloudFront for a short
    # window to absorb click storms, but never longer than 60s so a
    # destination URL change propagates within a minute.
    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 60

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = local.acm_arn == null
    acm_certificate_arn            = local.acm_arn
    ssl_support_method             = local.acm_arn == null ? null : "sni-only"
    minimum_protocol_version       = local.acm_arn == null ? null : "TLSv1.2_2021"
  }

  aliases = local.domain_aliases

  web_acl_id = var.waf_acl_arn != "" ? var.waf_acl_arn : null

  logging_config {
    bucket          = aws_s3_bucket.access_logs.bucket_domain_name
    include_cookies = false
    prefix          = "${var.environment}/"
  }

  tags = merge(var.tags, {
    Name        = "${var.project_name}-${var.environment}"
    Environment = var.environment
  })
}

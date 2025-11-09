resource "aws_cloudfront_origin_access_control" "url_shortener" {
  count = var.enable_cloudfront ? 1 : 0

  name                              = "${var.project_name}-origin-access-control"
  description                       = "Origin Access Control for URL Shortener"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "url_shortener" {
  count = var.enable_cloudfront ? 1 : 0

  origin {
    domain_name = aws_api_gateway_rest_api.url_shortener.invoke_url
    origin_id   = "api-gateway-origin"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "CloudFront distribution for URL Shortener"
  default_root_object = ""

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "api-gateway-origin"

    forwarded_values {
      query_string = true

      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  # If custom domain is provided
  dynamic "aliases" {
    for_each = var.cloudfront_domain_name != "" ? [var.cloudfront_domain_name] : []
    content {
      name = aliases.value
    }
  }

  # If custom certificate is provided
  dynamic "viewer_certificate" {
    for_each = var.cloudfront_certificate_arn != "" ? [var.cloudfront_certificate_arn] : []
    content {
      acm_certificate_arn      = viewer_certificate.value
      ssl_support_method       = "sni-only"
      minimum_protocol_version = "TLSv1.2_2021"
    }
  }
}

# Output the CloudFront domain name if enabled
output "cloudfront_domain_name" {
  description = "CloudFront domain name (if enabled)"
  value       = var.enable_cloudfront ? aws_cloudfront_distribution.url_shortener[0].domain_name : ""
}

output "cloudfront_enabled" {
  description = "Whether CloudFront is enabled"
  value       = var.enable_cloudfront
}

# Serverless AWS URL Shortener

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-green.svg)](https://quarkus.io/)
[![AWS](https://img.shields.io/badge/AWS-Serverless-orange.svg)](https://aws.amazon.com/)
[![Terraform](https://img.shields.io/badge/Terraform-1.x-purple.svg)](https://www.terraform.io/)
[![DynamoDB](https://img.shields.io/badge/DynamoDB-Serverless-blue.svg)](https://aws.amazon.com/dynamodb/)

A serverless URL shortener implementation built with Quarkus and deployed on AWS using a cloud-native architecture. The application demonstrates the use of serverless technologies for building scalable web applications.

## Project Overview

This URL shortener is implemented using AWS serverless technologies. The application is built with Quarkus for performance and deployed using Infrastructure as Code (Terraform) on AWS.

Key Characteristics:
- Serverless architecture with no server management required
- Automatic scaling based on demand
- Cost-effective with pay-per-use pricing model
- Built with Quarkus framework for optimal performance
- Infrastructure as Code with Terraform for reproducible deployments
- Comprehensive monitoring and logging capabilities

## Technical Architecture

The application follows a serverless, cloud-native approach using AWS services:

### AWS Lambda
The application logic is implemented as a serverless function that scales automatically with demand. Configuration includes:
- Java 21 runtime (or native runtime for optimized performance)
- 1024MB memory allocation
- 30-second timeout for request processing
- Environment variables for configuration management

### API Gateway
Handles HTTP routing and exposes the application endpoints with:
- Routes for URL shortening (`POST /shorten`) and redirection (`GET /{id}`)
- Integration with the Lambda function
- Deployment and staging management
- Custom domain support

### DynamoDB
Serverless NoSQL database for storing URL entries and click events:
- `url-entries` table: Stores shortened URLs and their original destinations
- `click-events` table: Tracks click analytics for each shortened URL
- Provisioned capacity for predictable performance

### CloudFront (Optional)
When enabled, CloudFront provides:
- Global content delivery with low latency
- HTTPS enforcement for security
- Caching of API responses for improved performance
- Custom domain support with SSL certificates

### IAM & CloudWatch
- IAM roles and policies for secure resource access
- CloudWatch for comprehensive logging and monitoring

## Features

- URL Shortening: Convert long URLs to short, shareable links
- Click Analytics: Track how many times each shortened URL is accessed
- Custom Domains: Support for custom domains via CloudFront
- High Availability: Built on AWS serverless services for maximum uptime
- Auto-scaling: Automatically scales to handle traffic spikes
- Cost Efficient: Pay only for what you use with serverless pricing

## Architecture Rationale

The serverless architecture was selected for this implementation based on the following considerations:

1. Operational Efficiency: No server management required, reducing operational overhead
2. Scalability: Automatic scaling to handle varying traffic patterns
3. Cost Optimization: Pay-per-use pricing model aligns with usage patterns
4. Performance: Quarkus framework provides fast startup times and low memory usage
5. Deployment Automation: Terraform enables reproducible, version-controlled deployments

## Getting Started

### Prerequisites
- Java 21+
- Maven
- AWS CLI configured
- Terraform 1.0+

### Running Locally
You can run your application in dev mode that enables live coding using:

```shell script
mvn quarkus:dev
```

### Packaging
The application can be packaged using:

```shell script
mvn package
```


### Creating a Native Executable
You can create a native executable using:

```shell script
mvn package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
mvn package -Dnative -Dquarkus.native.container-build=true
```

## Deployment

The application is deployed using Terraform with the following AWS resources:
- Lambda function for the application
- API Gateway for HTTP routing
- DynamoDB tables for data storage
- IAM roles and policies for permissions
- CloudFront distribution (optional) for CDN
- CloudWatch log groups for monitoring

To deploy the application, use the Terraform configuration in the `terraform/` directory.
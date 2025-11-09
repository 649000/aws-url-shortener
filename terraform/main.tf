terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "nazri-terraform-state"
    key            = "url-shortener/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}
# Get current workspace
locals {
  environment = terraform.workspace == "default" ? var.environment : terraform.workspace
}

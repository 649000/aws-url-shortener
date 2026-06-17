# Backend configuration for the prod environment.
# Pass this file to `terraform init`:
#   terraform init -backend-config=envs/prod/backend.hcl

bucket         = "nazri-terraform-state"
key            = "url-shortener/prod/terraform.tfstate"
region         = "ap-southeast-1"
dynamodb_table = "terraform-state-lock"
encrypt        = true

# Backend configuration for the dev environment.
# Pass this file to `terraform init`:
#   terraform init -backend-config=envs/dev/backend.hcl
#
# The S3 bucket and DynamoDB lock table must already exist. A typical
# one-time bootstrap:
#
#   aws s3api create-bucket \
#     --bucket nazri-terraform-state \
#     --region ap-southeast-1 \
#     --create-bucket-configuration LocationConstraint=ap-southeast-1
#
#   aws dynamodb create-table \
#     --table-name terraform-state-lock \
#     --attribute-definitions AttributeName=LockID,AttributeType=S \
#     --key-schema AttributeName=LockID,KeyType=HASH \
#     --billing-mode PAY_PER_REQUEST \
#     --region ap-southeast-1
#
# Replace <ACCOUNT_ID> with the AWS account ID where state should live.

bucket         = "nazri-terraform-state"
key            = "url-shortener/dev/terraform.tfstate"
region         = "ap-southeast-1"
dynamodb_table = "terraform-state-lock"
encrypt        = true

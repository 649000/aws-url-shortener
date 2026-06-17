# Terraform Infrastructure

This directory defines every AWS resource the URL shortener depends on, as
small reusable modules composed by per-environment root configurations.

## Layout

```
terraform/
├── modules/                  # reusable building blocks
│   ├── dynamodb/             # url-entries + click-events tables
│   ├── lambda/               # Quarkus jar -> Lambda + IAM + DLQ
│   ├── api-gateway/          # HTTP API v2 + routes + throttling + access logs
│   ├── cloudfront/           # optional CDN in front of the API
│   └── alarms/               # CloudWatch metric alarms + dashboard + SNS
└── envs/
    ├── dev/                  # dev entry-point + backend.hcl + dev.tfvars
    └── prod/                 # prod entry-point + backend.hcl + prod.tfvars
```

The split is intentional: the modules know nothing about each other, so you
can reuse `modules/lambda` or `modules/dynamodb` for a future service without
dragging in API Gateway.

## One-time bootstrap

The Terraform state lives in S3, locked by a DynamoDB table. Create both
once per AWS account:

```bash
# State bucket
aws s3api create-bucket \
  --bucket nazri-terraform-state \
  --region ap-southeast-1 \
  --create-bucket-configuration LocationConstraint=ap-southeast-1

aws s3api put-bucket-versioning \
  --bucket nazri-terraform-state \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket nazri-terraform-state \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# Lock table
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region ap-southeast-1
```

## OIDC for GitHub Actions (recommended)

GitHub workflows in `.github/workflows/` prefer OIDC over long-lived access
keys. Create the IAM role + trust policy once:

```bash
# 1. Create the role with a trust policy that lets GitHub assume it.
cat > trust-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:649000/aws-url-shortener:*"
      }
    }
  }]
}
EOF

aws iam create-role \
  --role-name github-actions-deploy-dev \
  --assume-role-policy-document file://trust-policy.json

# 2. Attach least-privilege permissions (Terraform-managed resources only).
cat > deploy-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": ["s3:PutObject","s3:GetObject","s3:ListBucket"], "Resource": ["arn:aws:s3:::nazri-terraform-state","arn:aws:s3:::nazri-terraform-state/*"] },
    { "Effect": "Allow", "Action": ["dynamodb:GetItem","dynamodb:PutItem","dynamodb:DeleteItem"], "Resource": "arn:aws:dynamodb:ap-southeast-1:<ACCOUNT_ID>:table/terraform-state-lock" },
    { "Effect": "Allow", "Action": ["lambda:UpdateFunctionCode","lambda:GetFunction"], "Resource": "arn:aws:lambda:ap-southeast-1:<ACCOUNT_ID>:function:aws-url-shortener-dev" },
    { "Effect": "Allow", "Action": ["s3:PutObject"], "Resource": "arn:aws:s3:::<DEV_LAMBDA_BUCKET>/*" },
    { "Effect": "Allow", "Action": ["cloudfront:CreateInvalidation"], "Resource": "*" }
  ]
}
EOF

aws iam put-role-policy \
  --role-name github-actions-deploy-dev \
  --policy-name deploy-dev \
  --policy-document file://deploy-policy.json
```

Repeat with a tighter `sub` condition for prod (e.g. `repo:649000/aws-url-shortener:ref:refs/heads/main`).
Add the role ARN as the `AWS_ROLE_TO_ASSUME` repository secret.

If you'd rather stay on access keys, set `AWS_ACCESS_KEY_ID` /
`AWS_SECRET_ACCESS_KEY` and the workflows will fall back automatically.

## Day-to-day workflow

```bash
# Dev
cd terraform/envs/dev
terraform init -backend-config=backend.hcl
terraform plan -var-file=dev.tfvars
terraform apply -var-file=dev.tfvars -auto-approve

# Prod (manual approval gate in GitHub)
cd terraform/envs/prod
terraform init -backend-config=backend.hcl
terraform plan -var-file=prod.tfvars
terraform apply -var-file=prod.tfvars -auto-approve
```

## Static analysis

The CI workflow runs `terraform fmt -check -recursive`, `tflint`, and
`tfsec`. Locally:

```bash
terraform fmt -recursive
tflint --recursive --init
tfsec .
```

## Cost notes (ap-southeast-1, low traffic)

| Resource | Estimate |
|---|---|
| Lambda (1 GB·s, 200k invocations) | ~$0.50/mo |
| DynamoDB on-demand (URL entries + 90d clicks) | <$1/mo |
| API Gateway HTTP API (1M req) | ~$1/mo |
| CloudWatch logs + alarms | ~$0.50/mo |
| CloudFront (10 GB egress) | <$1/mo |
| **Total dev** | **~$0** (free tier) |
| **Total prod (low traffic)** | **~$3-5/mo** |

## Adding a new resource

1. Drop the resource in the appropriate `modules/*/main.tf` (or create a new
   module if it's a new logical group).
2. Expose any new knobs via `modules/*/variables.tf` and any new outputs via
   `modules/*/outputs.tf`.
3. Wire the new inputs in `envs/{dev,prod}/main.tf`. Keep `dev.tfvars` and
   `prod.tfvars` as the single source of truth for environment-specific values.
4. Run `terraform fmt -recursive && terraform validate` locally before
   opening a PR.

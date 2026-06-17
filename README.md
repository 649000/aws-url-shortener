# Serverless AWS URL Shortener

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-green.svg)](https://quarkus.io/)
[![AWS](https://img.shields.io/badge/AWS-Serverless-orange.svg)](https://aws.amazon.com/)
[![Terraform](https://img.shields.io/badge/Terraform-1.x-purple.svg)](https://www.terraform.io/)
[![CI](https://img.shields.io/badge/CI-GitHub_Actions-2088FF.svg)](./.github/workflows/ci.yml)
[![Tests](https://img.shields.io/badge/Tests-95_passing-brightgreen.svg)](./src/test)

A serverless URL shortener built on Quarkus + AWS Lambda, wired to
DynamoDB through API Gateway HTTP API (with optional CloudFront
in front), and managed end-to-end by Terraform. Every commit runs
through GitHub Actions: tests, lint, security scans, and a `terraform plan`
for the dev environment.

---

## Highlights

- Quarkus 3.x on AWS Lambda — both JVM (`java21`) and GraalVM
  native-image (`provided.al2`) build paths are first-class citizens.
- DynamoDB Enhanced Client with atomic click-counter updates (no
  read-modify-write race).
- Per-route throttling, access logs, custom domains, CORS baked into
  API Gateway.
- Observability by default: CloudWatch dashboard, metric alarms on
  Lambda errors, API 5xx, and DynamoDB throttles, all wired to a single
  SNS topic.
- Multi-environment Terraform: `modules/` for reusable building blocks,
  `envs/{dev,prod}` for composition, S3-backed remote state with a
  DynamoDB lock table.
- CI/CD on GitHub Actions: `terraform fmt`, `tflint`, `tfsec`, CodeQL,
  Maven tests with JaCoCo, then OIDC-authenticated deploys (manual gate
  for prod).

## Architecture

```
                                     ┌──────────────────────────┐
                                     │   CloudFront (optional)  │
                                     │  ┌──────┐  ┌──────────┐  │
                                     │  │ WAF  │  │ TLS 1.2  │  │
                                     │  └──────┘  └──────────┘  │
                                     └────────────┬─────────────┘
                                                  │ HTTPS
                                                  ▼
                                     ┌──────────────────────────┐
                                     │      API Gateway         │
                                     │       (HTTP API v2)      │
                                     │  · throttling            │
                                     │  · access logs           │
                                     │  · CORS                  │
                                     └────────────┬─────────────┘
                                                  │ AWS_PROXY (v2.0)
                                                  ▼
                                     ┌──────────────────────────┐
                                     │       AWS Lambda         │
                                     │   Quarkus runtime        │
                                     │   (java21 / native)      │
                                     └─────┬───────────────┬────┘
                                           │               │
                          ┌────────────────▼──┐    ┌──────▼──────────┐
                          │   DynamoDB         │    │   DynamoDB      │
                          │   url-entries      │    │   click-events  │
                          │  (PAY_PER_REQUEST) │    │ (TTL + Streams) │
                          └────────────────────┘    └─────────────────┘
```

## Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Runtime | Quarkus 3.x on AWS Lambda | Fast cold start, native option, familiar Jakarta EE APIs |
| Build | Maven | Standard Java toolchain |
| API | API Gateway HTTP API v2 | ~70% cheaper than REST API, simpler auth model |
| Data | DynamoDB (on-demand) | No capacity planning; atomic counters for click tracking |
| Edge | CloudFront (optional) | Anycast + edge cache for redirects |
| IaC | Terraform 1.x with `modules/` + `envs/` layout | Standard, reviewable, modular |
| CI | GitHub Actions | First-class with the GitHub repo; OIDC for AWS |

## Project Layout

```
.
├── src/main/java/com/nazri/urlshortener/
│   ├── resource/        # JAX-RS resources (UrlShortenerResource, AnalyticsResource)
│   ├── service/         # Business logic
│   ├── repository/      # DynamoDB Enhanced Client adapters
│   ├── model/           # UrlEntry, ClickEvent, UrlStatus
│   ├── dto/             # Request/response payloads + ErrorDTO
│   └── exception/       # GlobalExceptionMapper, ValidationExceptionMapper
├── src/test/            # 95 tests, ~88% line coverage (see target/site/jacoco)
├── terraform/
│   ├── modules/         # dynamodb, lambda, api-gateway, cloudfront, alarms
│   ├── envs/dev/        # Dev composition + backend.hcl + dev.tfvars
│   ├── envs/prod/       # Prod composition + backend.hcl + prod.tfvars
│   └── README.md        # Bootstrap, OIDC, day-to-day workflow
├── .github/
│   ├── workflows/
│   │   ├── ci.yml             # Build, test, fmt, validate, tflint, tfsec, plan
│   │   ├── deploy-dev.yml     # Auto-deploy on push to main
│   │   ├── deploy-prod.yml    # Manual approval-gated deploy
│   │   └── codeql.yml         # Weekly + per-PR security scan
│   └── dependabot.yml         # Maven, GitHub Actions, Terraform providers
└── README.md
```

## CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | PR + push to main | `mvn verify`, JaCoCo artifact upload, `terraform fmt -check`, `terraform validate`, `tflint`, `tfsec`, and `terraform plan` for dev (commented back on the PR) |
| `deploy-dev.yml` | Push to main | Build + zip the Quarkus Lambda, `terraform apply` for dev, smoke test |
| `deploy-prod.yml` | `workflow_dispatch` (typed confirmation) | Same as dev but for prod, with a manual approval gate in the GitHub Environment |
| `codeql.yml` | PR + push + weekly cron | Security-and-quality scans over Java, Actions, and Terraform |
| Dependabot | Weekly | Bumps Maven, GitHub Actions, and Terraform providers; groups minor + patch upgrades to avoid PR noise |

All AWS access uses GitHub OIDC when `AWS_ROLE_TO_ASSUME` is set, with a
graceful fallback to `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` secrets.

## Tests

```bash
mvn clean verify
```

- 95 tests across unit + integration (`@QuarkusTest` + RestAssured)
- ~88% line coverage, report at `target/site/jacoco/index.html`
- Includes happy path, validation failures, expired/inactive URLs, custom
  alias collisions, atomic-click-counter race coverage, and analytics
  delegation tests

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Terraform 1.6+
- AWS CLI (for deploys; not required for `mvn quarkus:dev`)

### Local development

```bash
# Live-coding mode with hot reload
mvn quarkus:dev

# Native build (requires GraalVM)
mvn package -Dnative

# Container-native build (no GraalVM install required)
mvn package -Dnative -Dquarkus.native.container-build=true
```

### Deploy

See [`terraform/README.md`](./terraform/README.md) for the full bootstrap
(S3 state bucket + DynamoDB lock table + IAM OIDC role) and the day-to-day
`terraform plan/apply` workflow.

## Observability

Every environment ships with a CloudWatch dashboard
(`{project}-{env}`) showing:

- Lambda: invocations, errors, throttles, duration
- API Gateway: request count, 4xx, 5xx, latency
- DynamoDB: consumed read/write capacity + throttled requests for both tables

A single SNS topic (`{project}-{env}-alarms`) fans out alarm
notifications. Subscribe your email or PagerDuty endpoint via
`alarm_email_endpoints` in the env's tfvars file.
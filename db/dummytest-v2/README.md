# EN-R-002 Dummy Test V2

Static, ready-to-use EN-R-002 test data for an AWS test environment. There is no generator, package-specific tool or config file.

Choose **one** path on a clean test database:

1. **Manual frontend:** apply only `sql/05-manual-bootstrap.sql`, then follow [manual-test.md](manual-test.md).
2. **Integration/display seed:** apply `sql/00`, `10`, `20`, `30`, `40`, optionally `50`, then load the matching DynamoDB/S3 fixtures.

Do not mix the paths. All identities and documents are fictional (`テストデータ / 架空書類 / 公式提出不可`).

## Coverage

| Entity | Count |
|---|---:|
| EN-R-002 projects | 2 (both method number `002`) |
| Participating companies | 6 |
| Sites / site users | 8 / 8 |
| Equipment / participation | 8 / 8 |
| Approved / open / rejected | 5 / 2 / 1 |
| Seeded monitoring records | 10 (M1/M2 for E01–E05) |
| Equipment and monitoring files | 111 S3 objects total |

See [test-data.md](test-data.md) for the complete catalogue and [manual-inputs.md](manual-inputs.md) for browser values.

## Layout

```text
reference/       complete Markdown form references; every source MD is one worksheet
fixtures/        upload files plus filled Markdown sheet sets
sql/             static sequential PostgreSQL seed, verification and reset
dynamodb/        table-specific AttributeValue JSON arrays
s3/manifest.json source file → object key mapping
dataset-map.json complete static entity map
expected-results.json seeded calculation values
```

## AWS prerequisites

Use dedicated test resources. Never point these commands at production.

```bash
export TEST_DATABASE_URL='postgresql://...'
export AWS_REGION='ap-northeast-1'
export TEST_S3_BUCKET='...'

export DDB_PROJECT_PLAN='...'
export DDB_APPLICATION_CONFIRMATION='...'
export DDB_SUSTAINABILITY='...'
export DDB_PARTICIPATION_REVIEW='...'
export DDB_REVIEW_HISTORY='...'
export DDB_MONITORING='...'
```

Confirm the account before writing:

```bash
aws sts get-caller-identity
psql "$TEST_DATABASE_URL" -c 'select current_database(), current_user;'
```

On EC2, do not set `AWS_PROFILE` or static access keys. The backend and AWS CLI use the EC2 instance role. PostgreSQL credentials remain separate unless RDS IAM authentication is configured.

The EC2 role used to execute this package needs scoped access to the test resources, including `ses:SendEmail`, DynamoDB `DescribeTable`/`PutItem`/`GetItem`, and S3 `PutObject`/`GetObject`/`HeadObject`. Scope bucket, object prefix and table ARNs to the test environment.

## SES sandbox email policy

The test environment has six verified receiver addresses. Do not reuse them as every application's stored email: application account emails must remain unique.

| Email type | Unique in application | SES-verified in sandbox | V2 treatment |
|---|---:|---:|---|
| SES sender | N/A | Verified email or domain | `MAIL_FROM`, e.g. `notification@domain.jp` |
| Logical account email | Yes | No, while override is active | Unique address on a DNS-resolvable test domain |
| Actual delivery receiver | No application identity required | Yes | One of `<SES_RECEIVER_01>`…`<SES_RECEIVER_06>` |

Default manual-test configuration:

```bash
export MAIL_PROVIDER=ses
export MAIL_FROM='notification@domain.jp'
export APP_MAIL_OVERRIDE_TO='<SES_RECEIVER_01>'
export AWS_REGION='ap-northeast-1'
export MAIL_SES_REGION='ap-northeast-1'
```

- Any sender local part is valid when its domain is a verified SES identity in the same region.
- Set `APP_MAIL_OVERRIDE_TO`, not only `MAIL_OVERRIDE_TO`; the latter placeholder is commented out in `application.properties`.
- Keep all six real receiver addresses outside the repository. Record their slot mapping in private test evidence.
- Restart/redeploy the backend after changing the override.
- Disable the override only for a direct-delivery smoke test whose stored account email is one of the six verified receivers.
- DocuSign recipients are separate and do not inherit this SES override.

SES/role preflight:

```bash
aws sts get-caller-identity
aws sesv2 get-account --region "$AWS_REGION"
aws sesv2 get-email-identity --region "$AWS_REGION" --email-identity 'domain.jp'
aws sesv2 get-email-identity --region "$AWS_REGION" --email-identity '<SES_RECEIVER_01>'
```

Apply `db/migrations/20261016_remove_project_method_number_unique.sql` before `10-base.sql` when the deployed schema still has a unique constraint on `project.method_number`.

## Sequential PostgreSQL seed

Run from this directory:

```bash
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/00-preflight.sql
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/10-base.sql
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/20-companies.sql
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/30-participations.sql
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/40-monitoring-ready.sql
```

Stages are deltas and contain no repeated inserts. They stop on an unexpected predecessor or duplicate V2 identity. `50-snapshot.sql` is optional and writes simulated batch results; it does not invoke the application batch.

```bash
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/50-snapshot.sql
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/verify.sql
```

## DynamoDB import

Each file is a JSON array of DynamoDB AttributeValue items. This helper is typed in the shell; it is not package code:

```bash
put_items () {
  table="$1"; file="$2"
  jq -c '.[]' "$file" | while IFS= read -r item; do
    aws dynamodb put-item --region "$AWS_REGION" \
      --table-name "$table" --item "$item"
  done
}

put_items "$DDB_PROJECT_PLAN" dynamodb/project-plan.json
put_items "$DDB_APPLICATION_CONFIRMATION" dynamodb/application-confirmation.json
put_items "$DDB_SUSTAINABILITY" dynamodb/sustainability.json
put_items "$DDB_PARTICIPATION_REVIEW" dynamodb/participation-review.json
put_items "$DDB_REVIEW_HISTORY" dynamodb/review-history.json
put_items "$DDB_MONITORING" dynamodb/monitoring-wave-1.json
put_items "$DDB_MONITORING" dynamodb/monitoring-wave-2.json
```

Import `monitoring-snapshot.json` **instead of** the two wave files when the test needs the simulated post-processing form with `processedAt`.

## S3 upload

The SQL S3 keys and DynamoDB evidence URLs match `s3/manifest.json`.

```bash
jq -c '.[]' s3/manifest.json | while IFS= read -r row; do
  file=$(jq -r '.file' <<<"$row")
  key=$(jq -r '.key' <<<"$row")
  type=$(jq -r '.contentType' <<<"$row")
  aws s3api put-object --region "$AWS_REGION" \
    --bucket "$TEST_S3_BUCKET" --key "$key" --body "$file" --content-type "$type"
done
```

Verify a sample:

```bash
aws s3api head-object --region "$AWS_REGION" \
  --bucket "$TEST_S3_BUCKET" --key dummytest-v2/enr002/equipment/E01/pcs_spec.pdf
```

## Reset

`reset.sql` removes only V2 SQL identities. Review the target again before running it. DynamoDB and S3 removal is intentionally not automated; delete only keys listed in the supplied JSON/manifest.

Records created through the manual browser flow use application-assigned IDs and are not covered by this static reset. Remove them through the application or dispose of the dedicated test database.

```bash
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/reset.sql
```

## Form completeness

`reference/source/` copies every available `format_reference` Markdown file. Each source file represents one worksheet. The three consolidated references retain every source sheet, and the fixture sheet directories embed the complete source body beneath filled dummy values:

- `fixtures/project-plan/P01-sheets/` and `P02-sheets/`: 18 sheets each.
- `fixtures/individual-activity/P01-sheets/`: 10 sheets, battery and no-battery kept separate.
- `fixtures/houkoku/P01-sheets/`: 7 sheets.

The original workbooks are not present, so these files preserve Markdown field/section parity, not Excel layout, validation or formulas.

## Known boundaries

- `example.test` identities are **seed-only**. They do not need direct delivery, but any email-triggering action on them requires `APP_MAIL_OVERRIDE_TO`.
- Browser-created account emails must be unique and use a test domain with an MX or A record. They need not be individually SES-verified while override is active.
- Manual monitoring dates must be between the actual UI-created activity start and today. Fixed 2026 dates are seed-only.
- `50-snapshot.sql` stores expected results directly and is not a real batch execution test.
- The canonical schema permits only one total row per `batch_id`; V2 therefore stores the non-zero P01 total only. P02 intentionally has no approved member in this dataset.

# Manual frontend test — AWS test environment

This is one start-to-finish golden path. Use [manual-inputs.md](manual-inputs.md) for values and [test-data.md](test-data.md) for the larger seeded catalogue.

## Before testing

- Confirm the application points to dedicated AWS test RDS, DynamoDB and S3 resources.
- Confirm the AWS account/region and that no V2 seed is already loaded.
- Replace every `example.test` address used in the browser with a unique logical address on a DNS-resolvable test domain.
- Set `MAIL_PROVIDER=ses`, a sender under the verified domain, and `APP_MAIL_OVERRIDE_TO=<SES_RECEIVER_01>`.
- Keep the actual six verified receiver addresses outside the repository. Confirm `<SES_RECEIVER_01>` is verified in the same SES region.
- Apply the method-number migration when needed.
- Run **only** the manual bootstrap:

```bash
psql "$TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f sql/05-manual-bootstrap.sql
```

Seed login: `it.admin` / `GxTest2026!Solar`.

### Email modes

**Normal E2E:** keep unique logical account emails and enable `APP_MAIL_OVERRIDE_TO`. All application emails arrive at the selected verified receiver.

**Direct-delivery smoke test:** disable the override and use one unused verified receiver as the stored email for one test account. Do not reuse it for another account. Re-enable the override after this test.

## 1. Create operator

1. Log in as `it.admin`.
2. Open operator-user management and create the operator from card 1.
3. Assign all permissions listed under that card.
4. Log out and confirm the new operator can log in.

**Check:** operator belongs to `OP000000001`; project create/read and company-review screens are visible.

## 2. Create both projects

1. Log in as admin/operator with project-create permission.
2. Create P01 using project card 2 and upload `fixtures/project-plan/P01-plan.xlsx`.
3. Create P02 using the same methodology `002`, but its own number/name, and upload P02 plan.
4. Open list and detail for both projects.

**Check:** P01 and P02 coexist; methodology is EN-R-002; baseline and post-project emissions are `0`; data manager is `鳥居 沙奈`; files can be downloaded.

## 3. Company signup and approval

1. Log out and open public signup.
2. Enter C01 from card 3, complete email verification if enabled, and submit.
3. Log in as admin and confirm C01 is pending.
4. Approve C01.
5. Open the delivered master setup link and create `master_c01`.

**Check:** pending becomes approved/setup-pending and finally active after setup; master can log in and sees only C01 data.

**Email check:** the message is physically delivered to `<SES_RECEIVER_01>` while the company retains its unique logical email. Record action, subject and timestamp because the override does not change the stored account email.

## 4. Sites and users

1. As C01 master, create S01 and S02 from card 4.
2. Invite/register `user_s01` and `user_s02`.
3. Edit each site and select the corresponding registered user as PIC.

**Check:** two sites appear under C01; each user sees only the assigned site; users from another company cannot read these sites.

## 5. Join P01 and register equipment

1. As C01 master, choose P01.
2. Register E01 for S01 using card 5 and upload all six applicable files.
3. Complete data-use confirmation, sustainability commitment and terms.
4. Submit participation.
5. Optionally repeat E02 for battery + subsidy coverage.

**Check:** participation is waiting for review; battery fields/files are visible; subsidy fields are hidden for E01 and visible for E02; uploaded files preview/download successfully for owner and reviewer.

## 6. Two-person review

1. As `it.admin`, perform primary review using card 6.
2. Log in as the different operator account.
3. Perform secondary review and approve.

**Check:** the same account cannot stand in for both review roles where separation is enforced; final state is `APPROVED`; project member count changes; activity start is the real approval/join date.

## 7. Monitoring

1. Log in as `user_s01` or C01 master.
2. Open monitoring registration and select actual E01.
3. Enter E01-M1 from card 7, replacing the date with a valid date from actual activity start through today.
4. Upload generation, reverse-flow, control and battery evidence.
5. Save and read the record back.
6. Add M2 only with a later valid date.

**Check:** values, equipment/site/project and evidence are unchanged after reload; a pre-activity or future date is rejected; a user assigned to another site cannot alter E01.

## 8. Display and permission checks

Check all of the following:

| Check | Expected |
|---|---|
| P01/P02 project list and detail | Both shown to authorized operator |
| C01 master | C01/S01/S02 only |
| S01 user | Assigned site/equipment only |
| Equipment evidence preview | Owner and authorized reviewer allowed |
| Unauthenticated preview | Denied |
| Unrelated company/site user | Denied |
| Provisional/nonexistent S3 key | Denied/not found |
| Monitoring list/detail | Actual E01 values and files shown |
| File download | Original dummy fixture opens |

## 9. Short variation tests

- **Battery + subsidy:** register E02 and verify both conditional sections/files.
- **No battery:** use E03 values from `test-data.md`; battery inputs/evidence must not be required.
- **Rejection:** reject a new participation with the card-6 reason and confirm the reason is visible for correction.
- **Second project:** join P02 with a new record and confirm P01 relationships do not leak into P02.
- **SES direct delivery:** temporarily clear `APP_MAIL_OVERRIDE_TO`, use one unused `<SES_RECEIVER_0N>` as the account email, trigger one security email, then restore the override.

## Evidence to retain

Record actual IDs and capture screenshots/API responses for project creation, company statuses, site/user assignment, equipment submission, both reviews, monitoring read-back and each denied permission case. Never paste real AWS secrets or personal email addresses into repository files.

# Compliance evidence screenshots

These images are referenced by `../compliance-report.md`.

| File | What it shows | Control |
|------|---------------|---------|
| `pushprotection rulesets.png` | Branch ruleset rules for `main`/`dev` (require PR, require status checks, block force pushes, restrict deletions) | 8.4 / 8.32 |
| `2FA.webp` | Organisation Authentication security: "Require two-factor authentication" enabled + secure methods only | 8.4 |
| `advanced security.png` | Advanced Security page: dependency graph enabled, Dependabot alerts enabled | 8.8 |
| `Code scanning.png` | Code scanning (CodeQL) settings **and** Secret Protection + Push protection enabled | 8.25 / 8.29 and 8.28 |
| `environments.png` | Environments list (`Test` and `Production`) | 8.31 |
| `environments cofiguration.png` | Production deployment protection rules (required reviewers, prevent self-review, wait timer, `main`-only, no admin bypass) | 8.31 |
| `Artifact and log.png` | Actions artifact and log retention (90 days) | 5.23 |
| `CodeQL.png` | Code scanning (CodeQL) findings list on `branch:main` — 18 open alerts, mostly in vendored third-party JS; evidence the SAST gate is live and producing results | 8.25 / 8.29 |

## SR-03 validation evidence

These images support `../mitigations/SR-03-confidential-appointment-access-control.md`.

| File | What it shows |
|------|---------------|
| `SR-03 test results.png` | Earlier API validation screenshot showing `167` tests passing |
| `SR-03 test results2.png` | Earlier re-test screenshot showing `168` tests passing |
| `SR-03 testresults3.png` | Final API-side validation screenshot showing `172` tests passing |
| `SR-03 testresults4.png` | Final full reactor build screenshot showing `126` omod tests and `BUILD SUCCESS` |

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

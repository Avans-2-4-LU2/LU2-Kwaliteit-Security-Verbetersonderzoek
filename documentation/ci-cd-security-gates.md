# CI/CD Security Gates

## Purpose

Documents the automated security gates that run in the pipeline, which of them block a merge (required
status checks), and how exceptions are handled. It complements `branch-protection-documentation.md`
(general branch rules) and supports NEN-7510 controls 8.25, 8.29, 8.8, and 8.32.

## Gates (run on every pull request to `main` / `dev`)

| Gate | Workflow | Required to merge |
|------|----------|-------------------|
| Build & unit tests (`mvn -B test`) | `maven-test.yml` | Yes (added after #80 merges) |
| SAST | `codeql.yml` | Yes |
| Dependency Review (blocks new vulnerable/disallowed-license deps) | `dependency-review.yml` | Yes |
| SBOM & SCA (CycloneDX SBOM + Grype scan) | `sbom.yml` | Yes (runs; non-blocking on the accepted dependency baseline) |
| Secret scanning + push protection | GitHub native | Yes (blocks the push) |

## Exceptions

False positives are not tracked in a separate log; they follow `security-alert-handling.md` — CodeQL /
Dependabot / Secret Scanning dismissed in the Security tab with a justification, Grype findings suppressed
in `sbom-analysis.md`. High/critical dismissals need a second contributor's approval.

## Note on the SCA gate

The Grype SCA scan is non-blocking on findings because the deployment baseline contains accepted critical
CVEs in platform-`provided` libraries (see `risk-assessment-report.md` §4.3). **New** vulnerable
dependencies are blocked by the Dependency Review gate, which evaluates only the PR diff.

## Evidence

Screenshot of the branch ruleset required-status-checks configuration:

<!-- To be added once #80 is merged and "Java CI with Maven / build" is set as a required check,
     so the screenshot shows the final, complete set of required checks. -->

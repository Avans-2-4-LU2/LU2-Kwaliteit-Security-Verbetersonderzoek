# Compliance Report - CI/CD Pipeline vs NEN-7510:2024-2

## 1. Purpose and scope

This report demonstrates how the project's software-development platform and CI/CD pipeline (GitHub repository, workflows, and supporting documentation) comply with the relevant NEN-7510:2024-2 controls. For each control it states what the control requires, how the pipeline satisfies it (with a reference to a concrete file or setting), and the residual risk that is not yet covered.

The control selection follows the relevant-controls list from workshop WS02 (chapters 5 and 8). The repository is hosted under the **Avans-2-4-LU2** GitHub organisation.

## 2. Status legend

- **Implemented** - the control is in place and evidenced by a file or setting in this repository.
- **Partial** - the control is partly in place; residual work remains.
- **Not yet** - the control is recognised but not yet implemented (with reason).

## 3. Note on evidence

Controls enforced through GitHub settings have been verified live (branch ruleset, Advanced Security, Environments) and screenshots are stored in `documentation/evidence/`. Two organisation-level items (MFA enforcement, SSO) and the Actions retention setting still need to be confirmed by an organisation owner and are flagged below.

---

## 4. Control compliance

### 8.4 & 8.32 - Source-code access & change management

- **Requires:** access to source code is restricted; all changes are traceable to a person and go through a controlled, reviewed change process.
- **How the pipeline complies:** a branch ruleset targeting `main` and `dev` (verified live) requires a pull request, requires status checks to pass, blocks force pushes, and restricts deletions. At least one approving review is required before merge. Commits follow Conventional Commits and link to issues for traceability (`commit-convention.md`); the review process is documented (`review-documentation.md`, `branch-protection-documentation.md`). Organisation-wide two-factor authentication is enforced, with only secure 2FA methods allowed (verified), so all access is tied to MFA-protected accounts.
- **Status:** Implemented.
- **Residual risk:** signed (PGP) commits are **not** required (ruleset confirmed off).

### 8.8 - Management of technical vulnerabilities

- **Requires:** vulnerabilities are identified, assessed, and remediated in a timely, risk-based way.
- **How the pipeline complies:** the **dependency graph** and **Dependabot alerts** are enabled (verified). SBOM generation + SCA scan (`sbom.yml`, `sbom.md`); a Dependency Review gate blocks PRs introducing new vulnerable/disallowed-license dependencies (`dependency-review.yml`, `dependency-review.md`); CodeQL SAST runs on push/PR (`codeql.yml`). Findings are verified, contextually scored, and prioritised (`sbom-analysis.md`, on its feature branch).
- **Status:** Implemented for identification/analysis; remediation of findings is planned for sprint 3.
- **Residual risk:** **Dependabot security updates (auto-patch PRs) are disabled** - enabling them would close the loop on available fixes. The Critical platform-dependency CVEs are accepted (ACCEPT in the SBOM analysis) because they live in provided OpenMRS libraries this module cannot patch.

### 8.9 - Configuration management

- **Requires:** configurations are managed, documented, and protected against unauthorised change.
- **How the pipeline complies:** the pipeline is defined as code under `.github/workflows/` (version-controlled, reviewed, auditable); the SBOM (`bom.json`, `sbom.md`) serves as a dependency/configuration inventory; environment configuration is documented (`environment-configuration.md`).
- **Status:** Partial.
- **Residual risk:** no formal configuration baseline beyond the SBOM; immutable-artifact handling not yet established.

### 8.16 - Monitoring of activities

- **Requires:** activities are logged and reviewed for anomalies.
- **How the pipeline complies:** GitHub provides the organisation audit log, Actions run history, and Dependabot alerts.
- **Status:** Partial.
- **Residual risk:** no export to a SIEM, no alerting/webhooks for critical events (secret use, workflow-file changes), and no defined review cadence.

### 8.25 & 8.29 - Security during development & security testing

- **Requires:** security is built in throughout development; security testing is performed and documented as part of the SDLC.
- **How the pipeline complies:** SAST (CodeQL) runs on push and pull request (`codeql.yml`), with a code-scanning failure threshold of "High or higher"; Dependency Review runs on every PR (`dependency-review.yml`); a penetration test is planned with scope, method, and rules of engagement (`pentest-plan.md`) and a demonstration test exists (`ConfidentialAppointmentAccessControlTest`).
- **Status:** Partial.
- **Residual risk:** DAST (e.g. OWASP ZAP) is not implemented; the penetration test is paused pending a build blocker and professor input (see `pentest-plan.md`). CodeQL runs with `build-mode: none`, so no CI stage performs a real compile (see §5).

### 8.28 - Secure coding

- **Requires:** defined secure-coding guidelines exist and are checked.
- **How the pipeline complies:** **GitHub Secret Scanning and Push Protection are enabled** (verified) - secrets are detected and commits containing them are blocked. Secret handling is documented (`environment-configuration.md`); dependencies are pinned to exact versions in Maven; code is reviewed on every PR.
- **Status:** Partial.
- **Residual risk:** no enforced linters (SpotBugs/PMD/Checkstyle) in CI and no formally documented secure-coding standard.

### 8.31 - Separation of development, test, and production (OTAP)

- **Requires:** development, test, and production environments are separated, with separate configuration and secrets.
- **How the pipeline complies:** GitHub Environments **`Test`** and **`Production`** exist (verified). Production has deployment protection rules: required reviewers (two named approvers) with **prevent self-review** (four-eyes), a 5-minute wait timer, deployment restricted to the `main` branch, and **administrators cannot bypass**. Each environment has a separate `ENVIRONMENT` variable (`test` / `production`). Documented in `environment-configuration.md`.
- **Status:** Implemented (as deployment governance).
- **Residual risk:** the environments are configured with separate **variables** but **no environment secrets yet** (none are needed until something is actually deployed). There is no running deployed instance - the Environments provide deployment governance, not a live application.

### 8.33 - Test data

- **Requires:** realistic test data is used, but not real (patient) data.
- **How the pipeline complies:** tests use synthetic, in-repository datasets (e.g. `standardAppointmentTestDataset.xml`); no production or real patient data is used (and none exists in this project).
- **Status:** Partial.
- **Residual risk:** no formal test-data policy document; a dedicated generator (e.g. Synthea) is not used because synthetic fixtures suffice for the current scope.

### 5.23 - Information security for the use of cloud services

- **Requires:** the cloud services used are recorded and their use is secured.
- **How the pipeline complies:** GitHub is the cloud development/CI platform, used under the **Avans-2-4-LU2 organisation**; access and configuration are documented (`environment-configuration.md`, `branch-protection-documentation.md`). Actions artifact/log retention is set to **90 days** (the organisation-enforced maximum).
- **Status:** Partial.
- **Residual risk:** SSO/SAML single sign-on is **not available** on the organisation's current (non-Enterprise) GitHub plan, so identity-provider integration is out of scope; access relies on individual GitHub accounts within the organisation.

### 5.30 - ICT readiness for business continuity (BCM)

- **Requires:** ability to recover and continue operations after an incident (backup, disaster recovery, RTO/RPO).
- **How the pipeline complies:** all code and pipeline definitions are version-controlled in Git, so history is recoverable and builds are reproducible.
- **Status:** Not yet (recognised as lower priority for this project per WS02).
- **Residual risk:** no repository mirror/backup to own infrastructure, and no documented disaster-recovery/rollback procedure or RTO/RPO.

---

## 5. Incidental finding (pipeline gap)

During penetration-test setup it was found that no CI stage performs a real `javac` compile (CodeQL uses `build-mode: none`; the SBOM and Dependency-Review steps only read the dependency graph). As a result a compile-breaking defect (a UTF-8 BOM in committed `.java` source) was never caught by the pipeline. **Recommendation (sprint 3):** add a build + unit-test stage to CI as a quality gate, and remove the BOM(s). This strengthens controls 8.29 and 8.9.

## 6. Residual-risk summary

| Area | Residual risk | Control |
|------|---------------|---------|
| Change management | Signed (PGP) commits not required | 8.4 / 8.32 |
| Vulnerability remediation | Dependabot security updates off; unfixable platform CVEs accepted | 8.8 |
| Monitoring | No SIEM export or alerting on critical events | 8.16 |
| Security testing | No DAST; pentest paused; no real compile/test gate in CI | 8.25 / 8.29 |
| Secure coding | No enforced linters or documented coding standard | 8.28 |
| Environment separation | No environment secrets / no deployed instance yet | 8.31 |
| Cloud services | SSO unconfirmed (needs org owner); retention set to 90 days (org max) | 5.23 |
| Business continuity | No backup/DR procedure, no RTO/RPO | 5.30 |

## 7. Evidence

Screenshots stored in `documentation/evidence/`:

- [x] Branch ruleset (`main`/`dev`) - rules verified - 8.4 / 8.32
- [x] Advanced Security: Dependency graph + Dependabot alerts - 8.8
- [x] Secret Scanning + Push Protection enabled - 8.28
- [x] Code scanning (CodeQL) configuration - 8.25 / 8.29
- [x] Environments (`Test`/`Production`) + Production protection rules - 8.31
- [x] Actions artifact/log retention - 90 days (org-enforced max) - 5.23
- [x] Organisation MFA - enforced, secure methods only (verified) - 8.4
- [x] SSO / SAML - not available on current (non-Enterprise) plan - 5.23

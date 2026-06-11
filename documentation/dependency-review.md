# Dependency Review

## Purpose

The Dependency Review gate blocks any pull request that introduces a **new** vulnerable or disallowed-license dependency, before it can be merged into `main` or `dev`. It is the pull-request-level complement to the existing supply-chain controls:

- **SBOM / SCA** (`sbom.md`) inventories and analyses the dependencies *already* in the project.
- **Dependency Review** (this control) checks the dependency *changes* in each PR and stops new risk from entering.

**NEN-7510:2024-2 controls:** 8.8 (Management of technical vulnerabilities), 8.29 (Security testing in development and acceptance), 8.32 (Change management).

---

## How it works

The workflow runs [`actions/dependency-review-action@v4`](.github/workflows/dependency-review.yml) on the `pull_request` event. The action compares the dependency graph of the PR's base branch against its head, determines which dependencies are **added or changed**, and checks each one against the GitHub Advisory Database (vulnerabilities) and the configured license policy.

Because it evaluates only the dependency **diff** of the PR, the pre-existing platform CVEs documented in `sbom.md` (15 Critical, 61 High) do **not** trigger the gate - they exist in both the base and the head, so they are not "added". This is why a blocking threshold can be enabled here, while the full-tree SCA scan in `sbom.yml` remains advisory (`fail-build: false`) during the analysis sprint.

---

## Configuration

| Setting | Value |
|---------|-------|
| Workflow | `.github/workflows/dependency-review.yml` |
| Trigger | `pull_request` to `main`, `dev` |
| Vulnerability threshold | `fail-on-severity: high` |
| License policy | `deny-licenses: GPL-2.0, GPL-3.0, AGPL-3.0, LGPL-2.0` |
| PR feedback | `comment-summary-in-pr: on-failure` |
| Enforcement | Required status check `Review dependency changes` (see `branch-protection-documentation.md`) |

---

## Rationale

**Vulnerability threshold (`high`).** A PR is blocked when it adds a dependency with a High or Critical vulnerability - the same severity band the WS02 patch-priority guidance treats as urgent (≤ 1 week / ≤ 24 h). Medium and Low findings are reported but do not block, so the gate stops the most serious supply-chain risk without obstructing routine work.

**License policy (deny copyleft).** The denied licenses are the strong-copyleft ones (GPL, AGPL, LGPL). This is consistent with the license analysis in `sbom.md`, which found no GPL/AGPL in the project and noted that LGPL is only acceptable because the module is not distributed standalone. Blocking *new* copyleft dependencies preserves that distribution model. A deny-list is used rather than an allow-list so that the many platform components without a declared license are not falsely blocked.

**Enforcement as a required check.** Making `Review dependency changes` a required status check is the GitHub mechanism that satisfies the WS02 requirement that *all CI checks must pass before merge* (Opdracht 1 checklist; control 8.32). Without this, the check would only inform, not block.

---

## Verification

The gate was tested by adding a known-vulnerable dependency to a throwaway pull request:

- **Test dependency:** `org.apache.logging.log4j:log4j-core:2.14.1`
- **Result:** the `Review dependency changes` check **failed**, reporting 1 vulnerable package with 2 Critical and 1 High vulnerability (including CVE-2021-44228, "Log4Shell"). The merge was blocked.
- **Date:** 2026-06-11
- **Cleanup:** the test PR was closed without merging and the branch deleted; the vulnerable dependency never reached `dev` or `main`.

This confirms the gate blocks new vulnerable dependencies, not merely reports them. The two "unknown license" warnings shown in the same run refer to the GitHub Actions themselves (`actions/checkout`, `actions/dependency-review-action`); these are informational and are **not** blocked - the deny-list policy only fails on explicitly forbidden licenses, by design.

---

## NEN-7510:2024-2 mapping

| Control | How this control satisfies it |
|---------|-------------------------------|
| 8.8 | Prevents new known-vulnerable dependencies from entering the codebase |
| 8.29 | Automated security test run on every code change; PR check is the audit evidence |
| 8.32 | Enforced as a blocking gate on PRs into protected branches |

---

## Evidence

| Item | Location |
|------|----------|
| Workflow definition | `.github/workflows/dependency-review.yml` |
| PR check / comment output | Pull request "Review dependency changes" check (audit trail) |
| Blocking-test result | Failed check on the log4j-core 2.14.1 test PR (see Verification) |
| Required-check enforcement | Branch ruleset for `main` / `dev` (`branch-protection-documentation.md`) |

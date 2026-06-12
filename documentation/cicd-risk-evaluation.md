# CI/CD Risk Evaluation Report

## 1. Executive Summary

This report evaluates the CI/CD posture of the `LU2-Kwaliteit-Security-Verbetersonderzoek` project based on its repository configurations (GitHub Actions workflows) and associated development documentation.

While the project has documented solid security principles—such as environment separation, branch protection (NEN 7510 compliance), and utilizes automated tools like CodeQL and SBOM generation—there are significant gaps in the actual technical implementation. Key CI/CD elements like automated testing, blocking vulnerable dependency merges, and automated deployments are currently missing or misconfigured, leading to substantial operational and security risks.

---

## 2. Identified CI/CD Risks

### 2.1. High / Critical Risks

#### A. SCA Scan Does Not Break the Build (Vulnerability Passthrough)

- **Observation**: In the `.github/workflows/sbom.yml` file, the Anchore Grype SCA scan (`anchore/scan-action@v3`) is explicitly configured with `fail-build: false`.
- **Risk/Impact**: If a developer introduces a package with known critical vulnerabilities (CVEs), the pipeline will generate an alert but will successfully complete. Because the build does not fail, vulnerable code can seamlessly be merged into `develop` or `main` and eventually deployed, rendering the SCA security check passive.
- **Recommendation**: Change the configuration to `fail-build: true` and define a strict threshold (e.g., fail on `high` or `critical` severities) to ensure vulnerabilities block the pull request.

#### B. Absence of Core CI Pipeline (Build & Automated Tests)

- **Observation**: The `branch-protection-documentation.md` states: _"Requiring status checks ensures that automated tests and security scans must pass before any code is merged."_ However, there is no workflow in `.github/workflows/` configured to actually build the project (e.g., `mvn clean install`) or run unit/integration tests.
- **Risk/Impact**: Code can be merged without verifying functional correctness. The lack of automated tests in the pipeline severely increases the risk of deploying broken features or regressions to the test and production environments.
- **Recommendation**: Implement a standard CI workflow (e.g., `ci.yml`) that compiles the code and runs the Maven test suite. Enforce this workflow as a mandatory status check for all pull requests.

#### C. Missing Automated Deployment Pipeline (CD)

- **Observation**: The `environment-configuration.md` explicitly defines `test` and `production` environments, complete with approval gates, required reviewers, and separated secrets. Yet, there are no deployment workflows (e.g., `deploy.yml`) present in the repository.
- **Risk/Impact**: Deployments are likely being executed manually or via an undocumented shadow IT process. This bypasses the GitHub Actions environment protection rules, nullifies the automated approval gates, risks configuration drift, and lacks auditability.
- **Recommendation**: Implement an automated deployment workflow using GitHub Actions Environments. This ensures that the documented security controls (reviewers, secret isolation) are technically enforced.

#### D. Security Defects Are Not Automatically Enforced by CI/CD

- **Observation**: The repository lacks a CI/CD gate that validates secure coding issues and dependency findings on every change. Existing documentation claims status checks, but the current workflow set does not enforce checks for hardcoded secrets, input validation, or known high-risk library vulnerabilities.
- **Risk/Impact**: Critical application-level issues such as hardcoded credentials, SQL injection, XSS, and known vulnerable dependencies may be introduced and merged without being detected or blocked by the pipeline. This means the CI/CD system is not preventing insecure code from reaching test or production.
- **Recommendation**: Add or extend CI workflows to include security-specific gates: static analysis for secure coding patterns, secret scanning, dependency vulnerability enforcement, and mandatory build/test execution. Ensure these checks are required status checks on PRs so insecure code cannot be merged silently.

---

### 2.2. Medium Risks

#### A. Unpinned GitHub Actions (Supply Chain Risk)

- **Observation**: The GitHub workflows use mutable version tags for third-party actions (e.g., `actions/checkout@v4`, `github/codeql-action/init@v4`, `anchore/scan-action@v3`).
- **Risk/Impact**: If a malicious actor compromises a third-party action repository, they can overwrite the version tag to execute malicious code within this project's pipeline (e.g., stealing environment secrets or injecting backdoors into the build).
- **Recommendation**: Pin all third-party GitHub Actions to specific, immutable commit SHAs. Use tools like Dependabot or Renovate to manage updates to these SHAs automatically.

#### B. Ambiguous Status Check Enforcement

- **Observation**: While branch protection requires pull requests and 1 approval, the documentation's configuration table does not specify _which_ status checks are strictly required.
- **Risk/Impact**: Without explicitly defining mandatory status checks in the repository settings, a pull request could potentially be merged as long as it has an approval, even if the CodeQL or SBOM generation fails or is bypassed.
- **Recommendation**: Update the GitHub repository branch protection settings to explicitly require specific jobs (e.g., `Analyze (java-kotlin)`, `Generate SBOM and run SCA scan`) to pass before merging is permitted. Document these required checks.

#### C. Lack of Artifact Signing / Provenance

- **Observation**: The `sbom.yml` workflow generates and uploads an SBOM. However, there is no mechanism to cryptographically sign the SBOM or the resulting compiled artifacts.
- **Risk/Impact**: Consumers of the artifact (or deployment systems) cannot cryptographically verify that the code they are running was genuinely produced by this exact CI pipeline, making the system susceptible to tampering between the build and deployment phases.
- **Recommendation**: Adopt SLSA (Supply chain Levels for Software Artifacts) practices by utilizing tools like Sigstore's Cosign to sign the SBOM and build artifacts natively in the GitHub Actions pipeline.

---

### 2.3. Low Risks / Operational Observations

#### A. CodeQL Build Mode Optimization

- **Observation**: The `codeql.yml` workflow uses `build-mode: none` for `java-kotlin`.
- **Risk/Impact**: While CodeQL supports autobuild features that don't require an explicit build command, complex enterprise applications (like OpenMRS modules) with intricate `pom.xml` dependencies can sometimes be analyzed incompletely without explicit build instructions.
- **Recommendation**: Monitor CodeQL output to ensure full coverage. If warnings regarding missing classes appear, switch the Java matrix configuration to `build-mode: manual` and provide the exact Maven compilation commands.

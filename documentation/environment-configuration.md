
# Environment Configuration

## Purpose

To support a secure software development lifecycle and align with NEN 7510 principles, the project uses separate environments for testing and production. This separation reduces the risk of configuration mistakes, accidental data exposure, and unauthorized deployments.

---

# Branching Strategy

The repository uses the following branching strategy:

| Branch    | Purpose                                 |
| --------- | --------------------------------------- |
| main      | Production-ready code                   |
| develop   | Integration and testing branch          |
| feature/* | Individual feature and task development |

### Workflow

1. Developers create a feature branch from `develop`.
2. Changes are implemented and committed to the feature branch.
3. A Pull Request is created towards `develop`.
4. After review and successful CI checks, the changes are merged into `develop`.
5. Validated changes are merged into `main`.
6. Deployments to the production environment are only allowed from `main`.

This workflow ensures traceability, peer review, and controlled promotion of code changes.

---

# Test Environment

## Purpose

The test environment is used to validate code changes before they are promoted to production.

## Configuration

| Setting              | Value            |
| -------------------- | ---------------- |
| Environment Name     | test             |
| Deployment Branch    | develop          |
| Required Reviewers   | No               |
| Administrator Bypass | Disabled         |
| Environment Variable | ENVIRONMENT=test |


## Security Rationale

The test environment is used to validate changes before they are promoted to production. It uses separate configuration and secrets to prevent accidental access to production resources.

Deployments are restricted to the `develop` branch so that only reviewed and integrated code is tested. Administrator bypass is disabled to ensure that all users, including repository administrators, follow the same deployment controls and cannot bypass the defined development process.

These measures improve consistency, traceability, and adherence to secure software development practices.

---

# Production Environment

## Purpose

The production environment represents the final deployment target.

## Configuration

| Setting              | Value                  |
| -------------------- | ---------------------- |
| Environment Name     | production             |
| Deployment Branch    | main                   |
| Required Reviewers   | Yes                    |
| Administrator Bypass | Disabled               |
| Environment Variable | ENVIRONMENT=production |


## Security Rationale

The production environment contains the final version of the application and therefore requires stricter controls than the test environment.

Deployments are restricted to the `main` branch to ensure that only reviewed and approved code reaches production. Production deployments require manual approval, reducing the risk of unauthorized or accidental changes.

Separate environment secrets are used to prevent exposure of sensitive production credentials. Administrator bypass is disabled to ensure that all users follow the same deployment approval process.

---

# Environment Separation

The project separates test and production environments through:

* Different deployment branches
* Different configuration variables
* Different secrets
* Different approval requirements

This minimizes the risk of test configurations, test credentials, or unvalidated code reaching production.

---

# Compliance Considerations

The environment setup supports secure development and change management practices by:

* Enforcing controlled deployments
* Requiring reviews for production changes
* Separating secrets and configuration
* Maintaining traceability through GitHub workflows and Pull Requests

These measures contribute to compliance with secure software development principles described in NEN 7510.

---


# Secret Management

Secrets (passwords, API keys, connection strings) are stored outside of version control and loaded at runtime via environment variables. They are never hardcoded in source files, `pom.xml`, or any file tracked by git.

Each environment uses its own isolated set of secrets, enforced through GitHub Actions environment secrets scoped per environment. This ensures test credentials never reach production and production credentials are never accessible during development or testing.

Secret scanning is enabled on the repository (GitHub Advanced Security) to automatically detect and alert on any credentials accidentally committed to source code.

**NEN-7510**: Separate environment secrets address access control (chapter 9) and secure development (chapter 14) by ensuring no single set of credentials spans environments and no credentials are exposed through source code or git history.

---

# Approval Gates

## Purpose

Approval gates are used to ensure that changes are reviewed and approved before they are merged into protected branches or deployed to production environments.

## Pull Request Approval

The repository uses branch protection rules to prevent direct changes to protected branches.

Configuration:

* Pull Requests are required before merging.
* At least one approval is required before merging.
* Force pushes are blocked.

This ensures that code changes are independently reviewed before being integrated into the project.

## Production Deployment Approval

The production environment requires manual approval before deployment.

Configuration:

* Production deployments require a reviewer.
* Administrator bypass is disabled.

This reduces the risk of accidental or unauthorized deployments and ensures that deployments follow the defined change management process.

## Security Rationale

The approval gates implement the four-eyes principle by requiring independent review before changes are merged or deployed. This improves traceability, supports change management, and reduces the risk of unauthorized or insecure changes reaching production.

# Code Scanning

## Purpose

CodeQL code scanning is configured to automatically detect security vulnerabilities
and coding errors before they reach protected branches or production.

## Configuration

| Setting        | Value                              |
| -------------- | ---------------------------------- |
| Setup Type     | Advanced setup                     |
| Languages      | Java / Kotlin, JavaScript / TypeScript |
| Query Suite    | Default                            |
| Scan on push   | main, Development                  |
| Scan on PR     | main, Development                  |
| Scheduled scan | Weekly (main)                      |

## Security Rationale

CodeQL scanning is integrated into the development workflow to identify
security vulnerabilities early in the development lifecycle. By scanning
on every Pull Request targeting `main` and `Development`, vulnerable code
is flagged before it is merged.

The scheduled weekly scan ensures that newly discovered vulnerabilities
are detected even without new commits, for example when new CVEs are
published for existing code patterns.

These measures support secure software development practices and contribute
to compliance with NEN 7510.

# Secret Scanning

## Purpose

Secret scanning is configured to automatically detect and prevent secrets
such as API keys, tokens, and credentials from being committed to the
repository. This reduces the risk of sensitive credentials being exposed
in the codebase or commit history.

## Configuration

| Setting          | Value    |
| ---------------- | -------- |
| Secret Scanning  | Enabled  |
| Push Protection  | Enabled  |

## How it works

**Secret Scanning** monitors the repository and commit history for known
secret patterns such as API keys, tokens, and credentials. When a secret
is detected, an alert is raised in the GitHub Security tab.

**Push Protection** actively blocks a push before it reaches the repository
if a secret is detected in the commit. This prevents secrets from ever
entering the codebase, rather than detecting them after the fact.

## Security Rationale

Accidentally committed secrets are a common source of security incidents.
By enabling Secret Scanning and Push Protection, the project ensures that
sensitive credentials are detected and blocked before they can be exposed.

Push Protection is the most valuable control as it prevents secrets from
entering the repository entirely, rather than detecting them after they
have already been committed and potentially exposed.

These measures support secure software development practices and contribute
to compliance with NEN 7510.

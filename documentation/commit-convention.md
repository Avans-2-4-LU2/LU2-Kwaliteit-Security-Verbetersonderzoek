# Commit Convention

## Purpose

To improve traceability, consistency, and maintainability, all commits in this project follow the Conventional Commits specification.

A standardized commit format makes it easier to understand code changes, review work, track project history, and link changes to requirements and GitHub Issues. This supports change management, auditability, and secure software development practices.

---

# Commit Structure

Commits should follow the structure below:

```text
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Components

#### Type

Describes the nature of the change.

Examples:

* feat
* fix
* docs
* test
* refactor
* ci
* security
* chore

#### Scope (optional)

Identifies the affected area of the system.

Examples:

```text
auth
patient
environment
security
codeql
dependabot
readme
```

#### Description

A short summary of the change written in English and using the imperative mood.

Good example:

```text
feat(environment): add production environment configuration
```

Bad example:

```text
added production environment configuration
```

#### Body (optional)

Used to provide additional context, implementation details, or rationale.

A body is recommended for:

* Security-related changes
* Infrastructure changes
* CI/CD changes
* Complex implementations
* Compliance-related changes

#### Footer (optional)

Used for references or additional metadata.

Examples:

```text
Refs: #25
```

```text
BREAKING CHANGE: deprecated API endpoint removed
```

---

# Allowed Types

## feat

Introduces new functionality.

Example:

```text
feat(patient): add patient search endpoint
```

---

## fix

Fixes a defect or bug.

Example:

```text
fix(auth): resolve token validation issue
```

---

## docs

Documentation changes only.

Example:

```text
docs(environment): add environment configuration documentation
```

---

## test

Adds or modifies tests.

Example:

```text
test(patient): add repository unit tests
```

---

## refactor

Improves code structure without changing functionality.

Example:

```text
refactor(service): simplify patient lookup logic
```

---

## ci

Changes to CI/CD pipelines or automation.

Example:

```text
ci(codeql): add security scanning workflow
```

---

## security

Security-related changes.

Example:

```text
security(secrets): separate test and production secrets
```

---

## chore

Maintenance tasks that do not directly affect functionality.

Example:

```text
chore(dependencies): update Maven dependencies
```

---

# Issue References

When applicable, commits should reference the related GitHub Issue.

Example:

```text
ci(codeql): configure security scanning workflow

Refs: #18
```

Linking commits to GitHub Issues improves traceability between:

```text
Requirement
↓
GitHub Issue
↓
Commit
↓
Pull Request
↓
Documentation
```

---

# Full Example

The example below demonstrates the use of a type, scope, body, and footer.

```text
security(environment): configure production approval gate

Configured the production environment to require
manual approval before deployment.

Administrator bypass has been disabled to ensure
that all users follow the same deployment process.

Separate production secrets have been configured
to reduce the risk of credential exposure.

Refs: #25
```

---

# Breaking Changes

Breaking changes should be indicated using an exclamation mark (`!`) after the type or scope.

Example:

```text
feat(api)!: remove deprecated patient endpoint
```

Breaking changes may also include a footer:

```text
feat(api): remove deprecated patient endpoint

BREAKING CHANGE: endpoint /v1/patient has been removed
```

---

# Compliance Rationale

Using a standardized commit strategy improves:

* Traceability of changes
* Code review efficiency
* Change management
* Auditability
* Team collaboration

By enforcing a consistent commit format and linking commits to GitHub Issues, the project maintains a clear and auditable history of software changes, supporting secure software development practices and compliance objectives.


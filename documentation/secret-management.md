
# Secret Management

## Purpose

To prevent unauthorized access to sensitive credentials and align with NEN 7510 principles, all application secrets (passwords, API keys, connection strings, certificates) must be stored outside of version control and loaded at runtime via environment variables.

This approach ensures that secrets are never exposed through source code, git history, or application logs.

---

# Inventory

The following secrets were identified in the codebase during the initial audit:

| Variable            | Description                              | Location (original)           |
| ------------------- | ---------------------------------------- | ----------------------------- |
| `HL7_EXPORT_HOST`   | Hostname of the HL7 reporting server     | `AppointmentActivator.java`   |
| `HL7_EXPORT_USER`   | Service account username for HL7 export  | `AppointmentActivator.java`   |
| `HL7_EXPORT_PASSWORD` | Password for HL7 export service account | `AppointmentActivator.java`   |

These were previously hardcoded as `private static final String` constants. They have been removed from the source code and replaced with environment variable lookups via `System.getenv()`.

---

# Storage Method

Secrets are managed through **environment variables**. This is the minimum required approach for this project. In a production environment, a dedicated secrets manager such as **Azure Key Vault** or **AWS Secrets Manager** is recommended for centralized, auditable, and encrypted secret storage.

| Environment      | Storage method                        |
| ---------------- | ------------------------------------- |
| Local development | `.env` file (not committed to git)   |
| Test / Production | GitHub Actions Secrets or Key Vault  |

---

# How to Configure Secrets Locally

1. Request the `.env` file from a team member via a secure channel (e.g. a password manager or encrypted message). **Do not share it over plain-text channels such as email or chat.**
2. Place the received `.env` file in the `openmrs-module-appointmentscheduling/` directory.
3. The `.env` file is excluded from git via `.gitignore`. **Never commit this file.**

To see which variables are required, refer to `.env.example` in the same directory.

---

# How to Add a New Secret

1. Add the variable name (without value) to `.env.example` with a short comment, and commit this change.
2. Add the actual value to your local `.env` file and share it with team members via a secure channel.
3. In Java code, read the value using `System.getenv("YOUR_VARIABLE_NAME")`.
4. Validate on startup that the variable is set (log a warning or throw if required).
5. Add the secret to the GitHub Actions environment or Key Vault for test/production deployments.

---

# Rotating a Secret

# Rotating a Secret

When a secret is compromised or needs to be replaced:

1. Generate a new credential in the target system (database, API, etc.).
2. Update the value in the relevant environment (GitHub Secrets, Key Vault, local `.env`).
3. Verify the application starts and functions correctly with the new value.
4. Invalidate the old credential in the target system immediately.


To remove secrets from git history entirely, tools such as [BFG Repo Cleaner](https://rtyley.github.io/bfg-repo-cleaner/) or `git filter-repo` can be used. This requires coordination with all contributors to re-clone the repository after the history rewrite.

---

# Compliance Considerations

This secret management approach supports the following NEN 7510 controls:

| NEN 7510 Chapter | Control                                        | How this addresses it                                              |
| ---------------- | ---------------------------------------------- | ------------------------------------------------------------------ |
| Chapter 9        | Access Control                                 | Secrets only accessible to authorized processes via env vars       |
| Chapter 10       | Cryptography                                   | Secrets never stored in plain text in source code or repository    |
| Chapter 12       | Operations Security                            | Secrets excluded from git backups; warning logged instead of value |
| Chapter 14       | System Acquisition, Development and Maintenance | Dev/test/prod environments use separate secrets; no prod access needed by developers |

---

# What NOT to Do

- Never hardcode secrets as constants or string literals in Java source files.
- Never commit `.env` files or any file containing real credentials.
- Never log secret values, even for debugging purposes.
- Never store secrets in `pom.xml`, `application.properties`, or any other file tracked by git.

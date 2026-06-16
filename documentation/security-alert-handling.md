# Security Alert Handling

## Purpose

This document describes the process for handling security alerts raised by GitHub security tooling, including CodeQL, Dependabot, Secret Scanning, and SBOM-based Software Composition Analysis (SCA) using Grype. It defines how alerts are reviewed, how false positives are identified and dismissed or suppressed, and how decisions are tracked for traceability.

This process supports compliance with NEN 7510-1:2024 and NEN 7510-2:2024, specifically the following controls from Bijlage A of NEN 7510-1:2024:

* **A.5.25** — Beoordelen van en besluiten over informatiebeveiligingsgebeurtenissen
* **A.5.28** — Verzamelen van bewijsmateriaal
* **A.8.8**  — Beheer van technische kwetsbaarheden
* **A.8.29** — Testen van de beveiliging tijdens ontwikkeling en acceptatie

---

## Alert Sources

| Tool             | Alert Type                       | NEN 7510-1:2024 Control |
| ---------------- | -------------------------------- | ----------------------- |
| CodeQL           | Code vulnerabilities (SAST)      | A.8.8, A.8.29           |
| Dependabot       | Dependency vulnerabilities (SCA) | A.8.8                   |
| Secret Scanning  | Exposed credentials and tokens   | A.5.25, A.5.28          |
| Grype (SBOM SCA) | Dependency vulnerabilities (SCA) | A.8.8, A.8.29           |

---

## Alert Review Process

1. A security alert is raised and becomes visible in the repository **Security tab**.
2. Any contributor reviews the alert and assesses whether it represents a real vulnerability or a false positive.
3. If the alert is a confirmed vulnerability, a fix is implemented via the standard Pull Request process.
4. If the alert is assessed as a false positive, it is dismissed following the process described below.

This process supports **A.5.25**, which requires that information security events are assessed and a decision is made on whether they should be categorized as information security incidents.

---

## False Positive Handling

A false positive is a security alert that does not represent an actual risk in the context of this project.

### Dismissal Process

1. Navigate to the repository **Security tab**.
2. Open the alert and review the finding in detail.
3. If the alert is determined to be a false positive, click **Dismiss**.
4. Select the appropriate dismissal reason:

| Reason                 | When to use                                                            |
| ---------------------- | ---------------------------------------------------------------------- |
| **False positive**     | The tool incorrectly flagged something that is not a vulnerability     |
| **Used in tests only** | The vulnerable code or dependency is only used in the test environment |
| **Not applicable**     | The vulnerability does not apply to this project's context             |

5. Add a **comment** explaining why the alert is considered a false positive. This is mandatory for traceability.

---

## Dependabot Alerts

Dependabot alerts are handled separately from CodeQL and Secret Scanning.

To dismiss a Dependabot alert:

1. Navigate to **Security → Dependabot alerts**.
2. Open the alert and review the finding.
3. Click **Dismiss alert** and select the appropriate reason.
4. Add a mandatory comment explaining the dismissal.

---

## SBOM/SCA Alerts (Grype)

SCA findings resulting from the Grype scan on the generated SBOM (`bom.json`) are handled outside of GitHub. Grype does not have native integration with the GitHub Security tab, meaning the standard dismissal workflow used for CodeQL and Dependabot is not available.

Instead, suppressions are documented in `documentation/sbom-analysis.md`. This file serves as the formal audit trail for decisions regarding Grype findings.

### Suppression Process

1. Run the Grype scan on the generated SBOM:

   ```bash
   grype sbom:./bom.json -o json --file sca-report/grype-report.json
   ```

2. Review the findings and determine which represent a real risk within the specific deployment context of this project.

3. If a finding is considered a false positive, it must be documented in `documentation/sbom-analysis.md` under the section **False Positive Analysis**, including the following required fields:

| Field          | Description                                                             |
| -------------- | ----------------------------------------------------------------------- |
| **ID**         | Unique identifier (e.g. FP-01)                                          |
| **CVE / GHSA** | Vulnerability identifier                                                |
| **Package**    | Name and version of the affected package                                |
| **Rationale**  | Written justification why the finding is not applicable in this context |
| **Decision**   | `SUPPRESS`, `PATCH`, or `ACCEPT`                                        |

4. If a finding is not classified as a false positive, a decision must be made:

   * `PATCH` (fix available)
   * `ACCEPT` (no fix available or out of scope; record in risk register)

---

### Conditional Suppressions

Some suppressions depend on the deployment configuration. If the configuration deviates from the standard (e.g. a non-standard log appender is enabled or an additional component is installed), the suppression must be re-evaluated and potentially removed. This must be explicitly documented in the rationale for each conditional suppression.

---

### Traceability

All decisions regarding Grype findings are traceable through the Git history of `documentation/sbom-analysis.md`. Every change in this file is linked to a commit, author, and timestamp.

This supports **A.5.28**, which requires that the organization establishes procedures for identifying, collecting, and preserving evidence related to information security events.

---

## Dismissal Authority

Any contributor may propose a dismissal. For high or critical severity alerts, dismissal must be reviewed and approved by at least one other contributor before execution.

---

## Traceability

All dismissals for CodeQL, Dependabot, and Secret Scanning alerts are automatically logged in the GitHub Security tab audit log.

Decisions regarding SBOM/SCA findings from Grype are recorded in `documentation/sbom-analysis.md` and tracked via Git history.

Dismissed alerts can be reviewed at any time via:

* **Security → Code scanning alerts → Filter: Dismissed**
* **Security → Dependabot alerts → Filter: Dismissed**
* **Security → Secret Scanning alerts → Filter: Dismissed**

This supports **A.5.28**, which requires that the organization establishes and implements procedures for identifying, collecting, and preserving evidence related to information security events.

---

## Compliance Rationale

| NEN 7510-1:2024 Control                                                        | Requirement                                                                             | How this process contributes                                                                                                                    |
| ------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| A.8.8 — Beheer van technische kwetsbaarheden                                   | Evaluate exposure to technical vulnerabilities and take appropriate measures            | Alerts from CodeQL and Dependabot are reviewed and handled in a structured process                                                              |
| A.8.8 — Beheer van technische kwetsbaarheden                                   | Evaluate exposure to technical vulnerabilities and take appropriate measures            | Grype SBOM scanning identifies vulnerabilities in dependencies; findings are assessed and documented with a PATCH, ACCEPT, or SUPPRESS decision |
| A.8.29 — Testen van de beveiliging tijdens ontwikkeling en acceptatie          | Security testing processes must be defined and implemented in the development lifecycle | CodeQL runs on every pull request to detect vulnerabilities before merging                                                                      |
| A.5.25 — Beoordelen van en besluiten over informatiebeveiligingsgebeurtenissen | Information security events must be assessed and categorized                            | All alerts are assessed and either resolved or dismissed with justification                                                                     |
| A.5.28 — Verzamelen van bewijsmateriaal                                        | Procedures for collecting and preserving evidence must be established                   | All dismissals are logged in the GitHub Security audit log                                                                                      |

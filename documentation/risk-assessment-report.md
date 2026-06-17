# Risk Assessment Report (RAR) - OpenMRS Appointment Scheduling Module

**Requirement:** S-18 (Epic 7) - prove the identified risks are understood and under control.
**Status:** draft. This report consolidates existing analyses; it does not produce new analysis.

> **Scope note.** This report consolidates: the CIA/BIV analysis (#29), the SBOM/SCA analysis (#17), the
> CI/CD risk evaluation, the bow-tie analysis (#33), the threat model (#51, OWASP Threat Dragon / STRIDE),
> the compliance report (#27), and the security backlog (#35). The penetration test (#36, on the
> `Feature/pentest` branch) confirms finding F-01 and is referenced where relevant.

---

## 1. Executive Summary

### 1.1 Purpose and scope

This Risk Assessment Report brings the separate sprint-2 security analyses together into a single,
auditable picture of the risks affecting the `openmrs-module-appointmentscheduling` module. Its goal
is that a reader - including an external auditor - can see in one place **what we protect, which risks
exist, how large they are, what we decided to do about each, and where the supporting evidence lives.**

The scope covers three layers:

1. **The module's data and functionality** - appointment records, requests/notes, confidential
   appointment types, schedules and time slots, and the privileges that guard them.
2. **The deployment's third-party dependencies** - the libraries OpenMRS supplies at runtime
   (`provided` scope), which carry the bulk of the critical CVEs.
3. **The CI/CD process** that builds and ships the module.

### 1.2 Method

The report is a *consolidation*, not a new investigation. It combines:

| Risk lens | Source | Scoring approach |
|-----------|--------|------------------|
| Asset-based risks | CIA/BIV analysis (#29) | `Risk = Impact x Likelihood`, 1-25 scale, with per-category appetite thresholds (Section 6 of #29). Impact uses the maximum CIA score per asset (conservative). |
| Dependency / CVE risks | SBOM & SCA analysis (#17) | Contextual score `CVSS x (reachability x 0.4 + healthcare-impact x 0.4 + exploit x 0.2)`, after removing verified false positives. |
| Process risks | CI/CD risk evaluation | Qualitative high/medium/low based on observed pipeline configuration. |
| Threat-model threats | Threat model (#51, STRIDE) | Severity from OWASP Threat Dragon (High/Medium); mapped to the common band in §3.1. |

Each risk is then given a treatment decision (avoid / mitigate / transfer / accept) with justification,
mapped to a NEN-7510:2024-2 control, and linked to the evidence artifact that supports it.

### 1.3 Headline risk picture

**Asset risks (#29).** Four asset risks score in the **unacceptable** band (>=15) and must be mitigated
before the module is considered safe for release:

- Appointment records - **20** (confidentiality)
- Appointment requests and notes - **15** (confidentiality)
- Confidential appointment types - **15** (confidentiality)
- Appointment blocks and time slots - **15** (integrity, patient care)

The single highest residual risk is **unauthorised disclosure of patient-linked appointment data**.
The codebase defines a dedicated confidentiality privilege, but the control observations in #29 (Section 8)
note there is not yet evidence that it is enforced consistently across all UI and API entry points - an
access-control gap that the penetration test (#36) targets directly and that the threat model (#51)
independently flags as "missing function-level authorization" and IDOR.

**Dependency / CVE risks (#17).** Of 135 raw scanner findings, 4 Critical findings were verified as
false positives (mitigated by the JDK 8 deployment or non-default configuration) and removed.
**10 active Critical findings** remain, with contextual scores of 3.9-5.9. The highest are
deserialization flaws in `commons-collections`, `commons-fileupload`, and `spring-web` (**5.9** each).
Six have an available fix (PATCH); four are end-of-life or platform-blocked and are **accepted with
compensating controls**, carried into the residual-risk summary (Section 7). All Critical CVEs are in
`provided` (platform-supplied) dependencies, so remediation is constrained by OpenMRS platform
compatibility.

**Process risks (CI/CD).** The pipeline documents strong intent but has implementation gaps: the SCA
scan does not fail the build (`fail-build: false`), there is no automated build/unit-test stage, and no
automated deployment pipeline enforces the documented environment approval gates. These weaken the
assurance that what ships is actually what was reviewed and tested.

**Threat model (#51).** A STRIDE threat model corroborates the access-control gap (F-01) and the
dependency RCE risk, and surfaces three additional application-layer threats - stored/reflected XSS,
CSRF, and unencrypted data at rest - now tracked as T-01 to T-03 in the register.

### 1.4 Conclusion

The risks affecting the module are **identified, scored, and traceable to evidence**. The most serious
exposures - confidentiality of appointment data and the critical deserialization/XXE CVEs in
platform-supplied libraries - are understood and have defined treatments. They are **not yet fully under
control**: the unacceptable-band asset risks and the CI/CD enforcement gaps require the mitigations and
re-tests scheduled for sprint 3. The detailed register, treatment decisions, and cost estimates follow
in the sections below.

---

## 2. Sensitive Data and Crown Jewels

*Source: CIA/BIV analysis (#29), sections 2-4. This section summarises what the module protects;
the full inventory and per-asset CIA scoring live in #29.*

### 2.1 Legal basis - why this data is special-category

Appointment and scheduling data can reveal health-related information (who is seeing which provider,
for what, and when). Under **AVG (GDPR) Article 9** this qualifies as **special-category personal data**,
which requires the highest level of protection. This is the reason the project applies a **low risk
appetite** and stricter acceptance thresholds for confidentiality and patient-safety risks (#29 §6).
NEN 7510-1 clause 6.1.2 additionally requires this documented risk analysis as the basis of the ISMS.

### 2.2 Sensitive data processed by the module

The module processes the following sensitive or security-relevant data (full inventory with source-file
references in #29 §3):

| Data category | Why it is sensitive |
|---------------|---------------------|
| Patient identity & appointment linkage (patient/visit/appointment IDs, status history) | Directly identifies a patient and links them to care events. |
| Appointment notes & reasons (`reason`, `cancel_reason`, request notes) | Free text can disclose clinical context or private circumstances. |
| Confidential appointment types (the `confidential` flag) | Marks appointment categories intended to be restricted (e.g. "Initial HIV Clinic Appointment"). |
| Provider schedules & availability | Reveals clinic planning; must stay correct to avoid double-booking / missed care. |
| Audit & traceability metadata (creator, changed-by, voided-by, timestamps, reasons) | Security-relevant: supports accountability and forensic review. |
| Patient contact information (phone number person-attribute) | Personal contact data referenced by the UI. |

### 2.3 Crown jewels

The crown jewels are the assets whose compromise would most directly affect patient privacy, patient
safety, or clinical operations. CIA scores use a 1-5 scale (5 = critical); they are taken verbatim from
the CIA classification in #29 §4-§5.

| Asset | CIA (C / I / A) | Primary threat scenario |
|-------|:---------------:|-------------------------|
| Appointment records | **5 / 5 / 4** | Data breach; unauthorised access to patient-linked records. |
| Appointment requests and notes | **5 / 4 / 3** | Disclosure of treatment intent or care pathways. |
| Confidential appointment types | **5 / 4 / 2** | Misclassification exposes restricted appointment categories. |
| Appointment blocks and time slots | **3 / 5 / 5** | Double-booking or missed appointments from incorrect schedules. |
| Audit history & void/retire metadata | **3 / 4 / 3** | Removal/alteration of evidence after a security incident. |

**Implication for this report:** confidentiality is the dominant impact dimension - three of the five
crown jewels score **C=5**. This is why the highest-scoring risks in the register (Section 3) and the
deep-dive bow-tie analysis (#33) both centre on **unauthorised disclosure of appointment records**.

---

## 3. Consolidated Risk Register

This section brings the separate analyses into one register. It draws from three sources that each use
a different scoring method, so §3.1 first defines a common severity band, then §3.2-§3.4 list the risks
per source on that shared scale. Treatment decisions, justifications, and bow-tie links are in Section 4;
cost estimates in Section 5.

### 3.1 Normalising the scales

| Source | Native scoring | Mapped to common severity |
|--------|----------------|---------------------------|
| Asset risks (#29 §7) | `Impact x Likelihood`, 1-25, with appetite bands (#29 §6) | 15-25 = Critical · 10-14 = High · 6-9 = Medium · 1-5 = Low |
| Dependency / CVE risks (#17 §5) | Contextual score 0-9.8 (`CVSS x context`), after false-positive removal | >=7 = High · 4-6.9 = Medium · <4 = Low |
| Process risks (CI/CD evaluation) | Qualitative High / Medium / Low | used directly |

> **Note on the CVE band.** Generic CVSS rates all ten findings as *Critical* (9.1-9.8). After
> contextual adjustment for our deployment (all `provided` scope, intranet, JDK 8), their real severity
> drops to **Medium/Low** (3.9-5.9). The register uses the contextual band; the raw CVSS is kept in §3.3
> for traceability.

### 3.2 Asset risks (from #29 §7)

Scores and treatment-intent are taken verbatim from the risk register in #29 §7 (impact = the maximum
CIA score per asset; multiplied by a likelihood estimate).

| ID | Risk | Score (1-25) | Severity | Source |
|----|------|:------------:|----------|--------|
| A-01 | Unauthorised disclosure of appointment records | **20** | Critical | #29 §7 |
| A-02 | Disclosure of appointment requests and notes (clinical context) | **15** | Critical | #29 §7 |
| A-03 | Exposure of confidential appointment types to unprivileged users | **15** | Critical | #29 §7 |
| A-04 | Loss of schedule integrity (double-booking / missed appointments) | **15** | Critical | #29 §7 |
| A-05 | Provider schedule integrity / availability errors | 8 | Medium | #29 §7 |
| A-06 | Appointment status-history integrity (audit trail) | 8 | Medium | #29 §7 |
| A-07 | Audit-metadata tampering | 8 | Medium | #29 §7 |
| A-08 | Privilege misconfiguration in UI / access control | 8 | Medium | #29 §7 |

### 3.3 Dependency / CVE risks (from #17 §5)

The ten **active Critical** SCA findings (after the four false positives in #17 §4 were removed). Listed
by contextual score, highest first. IDs preserve the #17 finding labels.

| ID (#17) | CVE | Package | CVSS (raw) | Contextual | Severity | CWE |
|----------|-----|---------|:----------:|:----------:|----------|-----|
| C-02 | CVE-2016-1000027 | spring-web 3.0.5 | 9.8 | **5.9** | Medium | CWE-502 |
| C-03 | CVE-2015-7501 | commons-collections 3.2 | 9.8 | **5.9** | Medium | CWE-502 |
| C-04 | CVE-2016-1000031 | commons-fileupload 1.2.1 | 9.8 | **5.9** | Medium | CWE-502 |
| C-08 | CVE-2013-7285 | xstream 1.4.3 | 9.8 | **5.3** | Medium | CWE-78 |
| C-09 | CVE-2019-13990 | quartz 2.1.1 | 9.8 | **5.3** | Medium | CWE-611 |
| C-10 | CVE-2019-10202 | jackson-mapper-asl 1.5.0 | 9.8 | **4.9** | Medium | CWE-502 |
| C-11 | CVE-2020-10683 | dom4j 1.6.1 | 9.8 | **4.9** | Medium | CWE-611 |
| C-12 | CVE-2021-23926 | xmlbeans 2.3.0 | 9.1 | **4.2** | Medium | CWE-776 |
| C-14 | CVE-2022-0839 | liquibase-core 2.0.5 | 9.8 | **4.1** | Medium | CWE-611 |
| C-13 | CVE-2026-40076 | openmrs-web 1.9.9 | 9.4 | **3.9** | Low | CWE-22 |

*61 High, 54 Medium, and 5 Low findings remain pending individual analysis (#17 §6-§7) and are out of
scope for this register, which covers the Critical tier.*

### 3.4 Process risks (from the CI/CD risk evaluation)

| ID | Risk | Severity | Source |
|----|------|----------|--------|
| P-01 | SCA scan does not fail the build (`fail-build: false`) - vulnerable deps can merge | High | CI/CD eval §2.1.A |
| P-02 | No automated build / unit-test stage in CI | High | CI/CD eval §2.1.B |
| P-03 | No automated deployment pipeline - documented approval gates not technically enforced | High | CI/CD eval §2.1.C |
| P-04 | Security defects not automatically enforced by CI/CD | High | CI/CD eval §2.1.D |
| P-05 | Unpinned third-party GitHub Actions (supply-chain) | Medium | CI/CD eval §2.2.A |
| P-06 | Required status checks not explicitly defined | Medium | CI/CD eval §2.2.B |
| P-07 | No artifact / SBOM signing or provenance | Medium | CI/CD eval §2.2.C |
| P-08 | CodeQL `build-mode: none` may analyse incompletely | Low | CI/CD eval §2.3.A |

### 3.5 Application-security and threat-model findings (from #51)

The threat model (#51, OWASP Threat Dragon, STRIDE) analyses the module across three nodes - the
Appointment Web Interface, the Appointment Service Layer, and the database - and identifies seven
threats. Three corroborate risks already in this register, one is already covered by the CVE register
(§3.3), and three are new and added here. Severities are the Threat Dragon values (a 0-10 CVSS-style
score, not contextually adjusted like the CVEs in §3.3).

| ID | Risk (STRIDE type) | Severity | Source |
|----|--------------------|----------|--------|
| F-01 | Missing function-level authorization + IDOR: `AppointmentService` methods use empty `@Authorized()` (login-only) and accept object IDs without ownership checks - confidential appointments readable without the confidentiality privilege (broken access control) | High | #51 (T2, T5) + #29 §8 |
| T-01 | Stored/Reflected XSS in the appointment web interface (e.g. `chosenLocation`, `fromDate` rendered into JSP without encoding) | High (8.1) | #51 (T1) |
| T-02 | Cross-Site Request Forgery on state-changing appointment actions (no CSRF tokens / SameSite) | High (8.3) | #51 (T4) |
| T-03 | Unencrypted sensitive appointment/patient data at rest in the database | Medium (6.0) | #51 (T7) |

**Threat-model cross-reference** (so the register and #51 stay consistent):

| #51 threat | STRIDE | Register item |
|------------|--------|---------------|
| Missing function-level authorization | Tampering | F-01 / A-03 |
| Insecure direct object reference | Tampering | F-01 / A-01 |
| Unauthorized data disclosure (DB) | Information disclosure | A-01 |
| RCE via outdated dependencies | Elevation of privilege | CVE register §3.3 (esp. C-02/C-03/C-04) |
| Stored/Reflected XSS | Tampering | T-01 (new) |
| Cross-Site Request Forgery | Tampering | T-02 (new) |
| Unencrypted data at rest | Information disclosure | T-03 (new) |

> **F-01 is corroborated, not provisional.** It is now supported by two independent analyses - the
> control-gap observation in #29 §8 and the threat model #51 (the "missing function-level authorization"
> and "IDOR" threats). The penetration test (#36) will add the executable demonstration; only that
> reconciliation remains, so F-01 is treated as a confirmed finding. It maps to asset risks **A-01** and
> **A-03**.

---

## 4. Risk Treatment and Control Mapping

Each risk from Section 3 is assigned one of four treatment strategies (#29 §7), the **specific
NEN-7510:2024-2 control** its mitigation satisfies, and a justification. Control numbers are taken from
the source analyses (#17 §8 per-CVE mapping, the bow-tie #33, the compliance report #27) to keep the
mapping consistent across documents. A legend of the control numbers is at the end of this section.

**Treatment strategies:** *Avoid* (remove the activity/redesign) · *Mitigate* (reduce impact or
likelihood with controls) · *Transfer* (shift to another party) · *Accept* (tolerate residual risk
when low enough and formally approved).

### 4.1 Asset risks (treatment from #29 §7)

| ID | Score | Band | Treatment | NEN-7510 | Justification |
|----|:-----:|------|-----------|----------|---------------|
| A-01 | 20 | Critical | **Mitigate (required)** | 8.4, 5.15, 8.15 | Highest residual risk; special-category data. Enforce access control, privilege checks, and audit logging. Detailed barriers in §4.5 (bow-tie). |
| A-02 | 15 | Critical | **Mitigate (required)** | 8.4, 5.12 | Free-text clinical context must not leak. Enforce data classification + access control. |
| A-03 | 15 | Critical | **Mitigate (required)** | 8.4, 8.3 | Confidentiality privilege must be enforced consistently across **all** UI and API entry points (the gap behind F-01). |
| A-04 | 15 | Critical | **Mitigate (required)** | 8.26, 8.13 | Patient-safety integrity. Add data validation, concurrency control, and backup to protect schedule correctness. |
| A-05 | 8 | Medium | Mitigate (optional) | 8.26 | Mainly operational; add integrity checks and availability safeguards. |
| A-06 | 8 | Medium | Mitigate (recommended) | 8.15 | Ensure immutable logging and access restrictions for the audit trail. |
| A-07 | 8 | Medium | Mitigate (recommended) | 8.15 | Maintain tamper-resistance and access restrictions on audit metadata. |
| A-08 | 8 | Medium | Accept / Mitigate (optional) | 5.18 | Monitor during code review that privilege checks are correctly applied. |

The four Critical asset risks (A-01-A-04) are in the **unacceptable** band and may not be accepted
locally - they must be reduced before the module is considered safe for release (#29 §6/§7).

### 4.2 Dependency / CVE risks (treatment from #17 §5)

| ID (#17) | Package | Treatment | NEN-7510 | Justification |
|----------|---------|-----------|----------|---------------|
| C-03 | commons-collections | **PATCH** → 3.2.2 | 8.8, 8.28, 5.19 | Fix available, low effort. Highest-priority patch candidate. |
| C-04 | commons-fileupload | **PATCH** → 1.3.3 | 8.8, 8.28, 5.19 | Fix available. |
| C-08 | xstream | **PATCH** → 1.4.7+ | 8.8, 8.28, 5.19 | One upgrade clears most of xstream's 22+ CVEs. |
| C-09 | quartz | **PATCH** → 2.3.2 | 8.8, 8.28, 5.19 | Fix available; exploit needs write access to job config. |
| C-12 | xmlbeans | **PATCH** → 3.0.0 | 8.8, 5.19 | Fix available; low EPSS lowers urgency. |
| C-14 | liquibase-core | **PATCH** → 4.8.0 | 8.8, 5.19 | Fix available; changelogs are trusted internal resources. |
| C-02 | spring-web | **ACCEPT** | 8.8, 8.28 | Fix needs Spring 6.0.0, incompatible with OpenMRS 1.9.9. Compensating control: do not expose Spring RMI/serializing-exporter endpoints externally. |
| C-10 | jackson-mapper-asl | **ACCEPT** | 8.8, 5.19, 5.22 | EOL, no fix. Compensating control: restrict untrusted JSON deserialization at application level. |
| C-11 | dom4j | **ACCEPT** | 8.8, 5.19, 5.22 | EOL 1.x, fix blocked by platform. Compensating control: disable external entities in deployment XML parser config. |
| C-13 | openmrs-web | **ACCEPT** | 8.8, 8.29, 5.19 | No fix for 1.9.9; requires admin auth. Compensating control: restrict admin access, audit module uploads, monitor filesystem writes. |

**Practical limitation (student-project context).** All PATCH items are `provided` dependencies supplied
by the OpenMRS *platform*, not bundled by this module - so this project cannot upgrade them from within
the module's own `pom.xml`. As a student group we have no channel to the OpenMRS core team, and there is
no deployed instance to override library versions on. The **PATCH** decision therefore records that *a
fix exists and is recommended* (which is itself the valuable finding versus the ACCEPT items, where no
fix exists); **acting on it is out of our control** and belongs to whoever owns the platform/deployment.
For the purpose of this project these PATCH items are handled as: documented recommendation in the
security backlog (#35), with the residual risk accepted for now under the same compensating controls as
the ACCEPT items (Section 7). The four ACCEPT items carry compensating controls and are recorded as
accepted residual risk (Section 7).

### 4.3 Process risks (treatment from the CI/CD evaluation)

| ID | Treatment | NEN-7510 | Action |
|----|-----------|----------|--------|
| P-01 | **Mitigate** | 8.8, 8.29 | Set the SCA scan to `fail-build: true` with a high/critical threshold. |
| P-02 | **Mitigate** | 8.25, 8.29 | Add a build + unit-test workflow as a required status check. |
| P-03 | **Mitigate** | 8.31, 8.9 | Implement an automated deployment workflow using GitHub Environments so the approval gates are technically enforced. |
| P-04 | **Mitigate** | 8.28, 8.29 | Add security gates (secret scanning, dependency enforcement, build/test) as required checks. |
| P-05 | **Mitigate** | 5.19, 8.8 | Pin third-party Actions to immutable commit SHAs (manage via Dependabot/Renovate). |
| P-06 | **Mitigate** | 8.32 | Explicitly define the required status checks in branch protection and document them. |
| P-07 | **Mitigate** | 8.9, 8.28 | Sign the SBOM/artifacts (Cosign / SLSA provenance). |
| P-08 | Accept / Mitigate | 8.29 | Monitor CodeQL coverage; switch to `build-mode: manual` if classes are missed. |

### 4.4 Application-security and threat-model findings

| ID | Treatment | NEN-7510 | Justification |
|----|-----------|----------|---------------|
| F-01 | **Mitigate (in scope)** | 8.4, 8.29 | Add the confidentiality-privilege check to the core/REST retrieval paths (mirroring the reporting evaluators), then re-test red→green (#36, sprint 3). Reduces A-01/A-02/A-03. Corroborated by #51. |
| T-01 | Mitigate (recommended) - **deferred** | 8.28 | Output-encode JSP EL (`<c:out>`) and validate input. Deferred under the time-box (Section 5); residual accepted. |
| T-02 | Mitigate (recommended) - **deferred** | 8.28, 8.26 | Anti-CSRF tokens + SameSite cookies. Deferred under the time-box; residual accepted. |
| T-03 | Mitigate (recommended) - **deferred** | 8.24 | Encrypt sensitive data at rest (disk/column-level); defence-in-depth on top of DB access control. Deferred; residual accepted. |

### 4.5 Bow-tie deep dive - top risk A-01 (from #33)

The top event - **unauthorised access to / bulk extraction of patient appointment records** - is analysed
in detail in the bow-tie analysis (#33). It lays out the barriers on both sides of the event, each mapped
to a NEN-7510:2024-2 control:

**Preventive barriers (reduce likelihood):**

| Threat | Preventive barrier | NEN-7510 |
|--------|--------------------|----------|
| BOLA / IDOR | Object-level authorization | 8.4 |
| Compromised credentials | Role-based access control | 5.15, 8.5 |
| Insecure code | Secure CI/CD | 8.29, 8.32 |
| Privilege misconfiguration | Access-rights review | 5.18 |

**Recovery barriers (reduce impact):**

| Consequence | Recovery barrier | NEN-7510 |
|-------------|------------------|----------|
| Large-scale data breach | Anomaly detection | 8.15 |
| Loss of accountability | Audit logging | 8.15 |
| Regulatory sanctions | Incident-response plan | 5.24, 8.16 |

This makes A-01's "Mitigate (required)" treatment concrete: the privilege-enforcement work (F-01 / A-03)
is a *preventive* barrier, while the logging gap work scheduled for sprint 3 (#20/#21, control 8.15)
supplies the *recovery* barriers (anomaly detection, audit logging). The diagram is in
`evidence/bowtie-patient-data-access.png` (#33).

### 4.6 Control legend

5.12 classification of information · 5.15 access control · 5.18 access rights · 5.19 supplier
relationships · 5.22 monitoring/change-management of supplier services · 8.3 information access
restriction · 8.4 access to source code & data · 8.8 technical vulnerabilities · 8.9 configuration
management · 8.13 information backup · 8.15 logging · 8.24 use of cryptography · 8.25 secure development
lifecycle · 8.26
application security requirements · 8.28 secure coding · 8.29 security testing · 8.31 separation of
environments · 8.32 change management.

**Coverage check:** every finding in Section 3 has a treatment and maps to at least one specific control,
and the control numbers used here are the same ones evidenced in the compliance report (#27), so the RAR
and the compliance report stay consistent.

---

## 5. Cost Estimates for Mitigations

### 5.1 Method

In this project the cost of a mitigation is essentially **developer time**: all tooling used (GitHub
Actions, CodeQL, Dependabot, Secret Scanning, Grype/CycloneDX) is free or already included in the
organisation's plan, so there are no licensing costs. Estimates below are therefore expressed as effort,
combined with the risk-reduction value, to support prioritisation.

**Effort scale**

| Size | Indicative time | Typical work |
|------|-----------------|--------------|
| **S** | < 0.5 day | A config or documentation change. |
| **M** | 0.5-2 days | A code change with tests, or a new CI workflow. |
| **L** | > 2 days | New pipeline/infrastructure or an architectural change. |
| **External** | n/a to us | Not actionable by the team (platform-`provided` dependency). |

**Risk-reduction value**

- **High** - removes or substantially reduces a High- or Critical/unacceptable-band risk.
- **Medium** - a meaningful reduction of a Medium-band risk, or a partial reduction of a higher one.
- **Low** - marginal, optional, or purely defensive.

The value reflects the severity of the risk addressed; it is independent of whether the mitigation is
scheduled now or deferred (deferral is captured in the treatment, not the value).

### 5.2 Cost table

| ID | Mitigation | Effort | Risk reduction | Notes |
|----|------------|:------:|:--------------:|-------|
| A-01 / A-03 / F-01 | Enforce confidentiality privilege on core/REST retrieval paths + tests | **M** | **High** | Own-code fix; closes the top confidentiality gap. Best effort-to-value ratio in the report. |
| A-02 | Access control + classification on free-text notes | M | **High** | Critical-band risk (15); rides on the same access-control work as A-03, so high value at shared effort. |
| A-04 | Validation + concurrency control + backup for schedule data | **L** | High | Concurrency control is the costly part; validation alone is M. |
| A-05 | Integrity/availability safeguards on provider schedules | M | Low | Optional. |
| A-06 / A-07 | Immutable logging + access restriction on audit trail/metadata | M | Medium | Overlaps with the sprint-3 logging work (8.15). |
| A-08 | Periodic access-rights review of privilege config | S | Low | Process step, not code. |
| T-01 | Output-encode JSP + input validation (XSS) | M | **High** | High-severity threat (#51); deferred under the time-box, residual accepted. |
| T-02 | Anti-CSRF tokens + SameSite cookies | M | **High** | High-severity threat (#51); deferred under the time-box, residual accepted. |
| T-03 | Encrypt sensitive data at rest | **L** | Medium | Deferred; infrastructure/DB-level change. |
| C-03, C-04, C-08, C-09, C-12, C-14 (PATCH) | Document recommended version bump in backlog (#35) | **S** to document | High *if applied* | **Applying is External** (platform-`provided`); the team can only record the recommendation. |
| C-02 (ACCEPT) | Do not expose Spring serializing-exporter endpoints | S | Medium | Deployment/config guidance. |
| C-10 (ACCEPT) | Restrict untrusted JSON deserialization | M | Medium | Application-level guard. |
| C-11 (ACCEPT) | Disable XML external entities at parser config | S | Medium | Config hardening. |
| C-13 (ACCEPT) | Restrict admin access, audit module uploads, monitor FS writes | M | Medium | Requires admin compromise first; defensive. |
| P-01 | SCA `fail-build: true` + threshold | **S** | **High** | One-line change; turns a passive scan into a real gate. Quick win. |
| P-02 | Add build + unit-test stage as required check | M | High | Also catches the BOM defect; enables coverage (#23). |
| P-03 | Automated deployment via Environments | **L** | Medium | Largest CI/CD item; enforces the documented gates. |
| P-04 | Add security gates as required checks | M | High | Bundles with P-01/P-02. |
| P-05 | Pin Actions to commit SHAs | S | Medium | Mechanical; can be automated via Dependabot. |
| P-06 | Define + document required status checks | S | Medium | Settings + documentation. |
| P-07 | Sign SBOM/artifacts (Cosign/SLSA) | M | Low | Provenance; lower priority for this scope. |
| P-08 | Monitor CodeQL coverage / `build-mode: manual` | S | Low | Operational watch item. |

### 5.3 Quick wins (low cost, high value)

Three mitigations give the most risk reduction for the least effort and should be prioritised first:

1. **P-01** (S, High) - make the SCA scan actually block the build.
2. **A-01 / A-03 / F-01** (M, High) - enforce the confidentiality privilege; the single most important
   own-code fix.
3. **P-02** (M, High) - add a build/test stage, which also unblocks the BOM fix and code coverage.

---

## 6. Security Backlog (from #35)

The prioritised security backlog lives in `security-requirements-mapping.md` (#35). It translates the
risks in this report into eight actionable security requirements (SR-01 to SR-08), each with tasks,
acceptance criteria, and NEN-7510 mappings. This section links that backlog to the register IDs from
Section 3 and to the mitigation tracks, so every risk traces to a concrete work item and every backlog
item traces back to a risk.

### 6.1 Backlog-to-register mapping

| SR | Requirement (short) | Register risks | Backlog priority | Execution (this project) |
|----|---------------------|----------------|------------------|--------------------------|
| SR-01 | RBAC on all appointment data | A-01, A-03, F-01 | Must do | **M1** - in scope; re-tested via #36 |
| SR-02 | Protect sensitive notes / free-text fields | A-02 | Must do | **M1** - in scope (rides on SR-01) |
| SR-03 | Enforce confidential-type privilege at every entry point | A-03, F-01 | Must do | **M1** - in scope; #36 |
| SR-04 | Integrity controls for blocks / time slots | A-04 | Must do | **Deferred** (L effort) - residual accepted (Section 7) |
| SR-05 | Harden audit trail (immutable logging) | A-06, A-07 | Should do | **M4** - overlaps sprint-3 logging (8.15) |
| SR-06 | Secure coding for deserialization / injection | C-02/03/04/08, T-01 | Should do | Partial - covered by SR-07; XSS (T-01) deferred |
| SR-07 | Dependency remediation backlog | C-02 to C-14 | Must do | Documented; **applying = External** (platform-provided) |
| SR-08 | CI/CD security gates + branch protection | P-01, P-02, P-04, P-08 | Should do | **M2 / M3** - in scope |

### 6.2 Reconciliation: backlog priority vs time-boxed execution

The backlog's Must/Should priorities reflect the *ideal* order if effort were unconstrained. The actual
execution this project follows is the time-boxed scope from Section 5 (tracks M1-M4):

- **In scope now (M1-M4):** SR-01, SR-02, SR-03 (M1, access control), SR-08 (M2/M3, CI gates),
  SR-05 (M4, logging).
- **Deferred despite "Must do":** SR-04 (schedule integrity, L effort) and the *application* of SR-07's
  patches (platform-`provided`, outside our control). Both are recorded as accepted residual risk in
  Section 7 with justification - the backlog priority is retained so the gap stays visible, not hidden.

This is a deliberate, documented divergence: the backlog says *what should happen*, Section 7 records
*what we are accepting for now and why*.

---

## 7. Residual Risk

> **To be finalised after sprint 3.** Residual risk is what remains *after* the sprint-3 mitigations are
> attempted, so this section is completed once those outcomes are known (which mitigations landed and which
> were deferred). The forward references to this section from §1.3, §4.2, and §6.2 resolve here.

**Already certain (independent of the sprint-3 outcome):**

- **Platform-`provided` CVEs (C-02 to C-14) remain residual.** They live in libraries supplied by the
  OpenMRS platform, which this module cannot upgrade. They are accepted with the compensating controls in
  §4.2; the PATCH recommendations are recorded in the backlog (#35) for the platform/deployment owner.

**To be added after sprint 3:**

- Which in-scope mitigations (M1-M4) were actually completed versus deferred.
- Explicit justification for any High-value deferrals - in particular **T-01 (XSS)** and **T-02 (CSRF)**,
  which are High-value (§5.2) and must not be left as silent gaps.
- The final accepted-residual list, each item with a recommended follow-up.

---

## 8. Traceability

Every finding in this report links to at least one NEN-7510:2024-2 control and to a concrete evidence
artifact - either an **analysis document** (the source that identified/scored the risk) or a **platform
artifact** (a CI step, gate, branch rule, or test that implements/verifies a control). This matrix is the
index that the sprint-3 and sprint-4 artifacts attach to, and it feeds the sprint-4 traceability matrix
(S-25).

| Register IDs | NEN-7510 | Analysis evidence (document) | Platform artifact (CI / live / test) |
|--------------|----------|------------------------------|--------------------------------------|
| A-01, A-03, F-01 (access control) | 8.4, 8.3, 5.15, 8.29 | `cia-analysis.md` §7/§8, `bowtie-analysis.md`, threat model (#51), `pentest-report.md` (#36) | `ConfidentialAppointmentAccessControlTest.java` (#36); `PatientToAppointmentDataEvaluatorTest` (control test); branch ruleset verified in #27 |
| A-02 (sensitive free-text) | 8.4, 5.12 | `cia-analysis.md` §3/§7 | masking + log tests (sprint 3, SR-02) |
| A-04 (schedule integrity) | 8.26, 8.13 | `cia-analysis.md` §7 | validation/concurrency tests (deferred, SR-04) |
| A-06, A-07 (audit trail) | 8.15 | `cia-analysis.md` §7 | logging tests (sprint 3, SR-05) |
| A-05, A-08 | 8.26, 5.18 | `cia-analysis.md` §7/§8 | PR code review |
| C-02 to C-14 (CVEs) | 8.8, 8.28, 5.19 | `sbom-analysis.md` §4/§5 | `bom.json` (CycloneDX), `grype-report.json`, `sbom.yml` (SBOM+SCA), `dependency-review.yml` gate; Dependabot verified in #27 |
| T-01, T-02, T-03 (STRIDE) | 8.28, 8.26, 8.24 | threat model (#51) | CodeQL (`codeql.yml`) for injection-class; remainder deferred |
| P-01 to P-08 (CI/CD) | 8.8, 8.25, 8.29, 8.31, 8.9, 8.32 | `cicd-risk-evaluation.md` §2 | `.github/workflows/*`; branch ruleset, CodeQL, Dependabot, Environments, retention - all verified in `compliance-report.md` (#27) |

**Coverage:** every register ID (A-01-A-08, F-01, T-01-T-03, C-02-C-14, P-01-P-08) appears above with a
control and an artifact. Artifacts marked "sprint 3" / "deferred" are planned tests or PRs that will be
attached as the corresponding mitigations land; the verified platform settings (branch ruleset, CodeQL,
Dependabot, Environments, retention) are already evidenced with screenshots in `documentation/evidence/`
via the compliance report (#27).

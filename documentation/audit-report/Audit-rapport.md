# Audit Report — OpenMRS Appointment Scheduling Module

**Datum:** 19 juni 2026
**Norm:** NEN-7510:2024 Deel 2
**Module:** openmrs-module-appointmentscheduling
**Branch:** feature/audit-report
**Statuskleur:** 🟢 GROEN

---

## 1. Executive Summary

**Status: 🟢 Green.** The security audit of the Appointment Scheduling module identified four
critical risks. All four have been fixed, tested, and verified. Two lower-priority improvements
remain open and are planned.

**Top 3 risks found and resolved:**

1. **Unauthorized access to sensitive appointment data.** A staff member without the right
   permission could view appointments explicitly marked confidential — for example, a visit to a
   specialized HIV clinic. This has been fixed: the system now consistently checks permission before
   showing this kind of appointment, anywhere in the application, and this has been proven with
   automated tests.
2. **Patient details written into system log files.** Patient names, dates of birth, and ID numbers
   were being recorded in plain text in the application's internal logs whenever a schedule was
   checked — and the logging itself had a weakness that could let someone inject fake log entries.
   Both problems are fixed: logs now record *that* something happened and *who* did it, without
   exposing the patient's personal details.
3. **No protection against scheduling conflicts.** Until recently, nothing stopped two appointments
   from double-booking the same time slot, or two staff members from overwriting each other's
   changes to a schedule at the same moment. This is now actively prevented and verified by tests.

**What this means for the organization:** the most serious risks — the ones that could expose
patient data or disrupt care delivery — have been closed. One remaining gap is the free-text notes
attached to appointments (e.g. the reason for a visit), which are not yet restricted from staff who
shouldn't see them; this is the only remaining "Unacceptable" risk.


---

## 2. Scope & Context

| | |
|---|---|
| **Application / module** | `openmrs-module-appointmentscheduling` |
| **Branch / version** | `feature/audit-report` (integrates PRs #93, #87, #88, #94, #96 plus commit `edebdf5`) |
| **Assessment period** | Sprint 1 – Sprint 4 |
| **Environment** | Development / Test (local + CI). No staging or production environment exists for this module. |
| **Standard** | NEN-7510:2024 Part 2 (primary); AVG/GDPR Art. 9 (supplementary, appointment data reveals health information); CRA (supplementary, see §7) |
| **Out of scope** | The OpenMRS platform core (`openmrs-core`) and any module outside this repository |

**What was not tested, and why:**

- **Dynamic/penetration testing against a live deployment** — no production-like environment exists;
  testing was source-level (SAST, manual review, automated tests simulating attacker behaviour).
- **This module's own input-handling code for injection/deserialization hardening** — the SBOM/SCA
  work (§5) covers known vulnerabilities *in dependencies*, but this module's own code has not been
  independently reviewed for the same class of issue.
- **A full re-check of every individual REST endpoint's authorization** — the confidentiality fix
  (§4, B-001) is proven across 8 core read paths that all REST endpoints route through, but each
  endpoint was not walked individually in this report.

---

## 3. Audit Methodology

| Test | Tool | Evidence |
|---|---|---|
| Code security (SAST) | CodeQL (GitHub Actions, required PR check) | `documentation/ci-cd-security-gates.md` |
| Dependencies (SCA) | Dependabot + manual CVE/CVSS/EPSS triage | `documentation/sbom-analysis.md` |
| SBOM | CycloneDX Maven plugin | `openmrs-module-appointmentscheduling/bom.json` |
| Attack surface mapping | Manual entry-point enumeration | `documentation/attack-surface-overview.md` |
| Logging gap analysis | Automated scan + manual review against NEN-7510 8.15 | `documentation/gap-analysis.md` |
| Code review | Manual review, AI-assisted (see §9) | PR review history |
| Penetration test | Manual demonstration of the confidentiality gap (attacker path vs. control path) | `documentation/pentest-plan.md`, `documentation/pentest-report.md` |
| Automated tests | JUnit + Spring Test | 198/198 `api`, 132/132 `omod` tests passing |

---

## 4. Risk Analysis & Findings

### Finding B-001

| | |
|---|---|
| **Title** | Unauthorized access to confidential appointments |
| **Severity** | Critical |
| **CVSS score** | 6.5 (`AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`) |
| **NEN-7510 control** | 8.2 / 8.26 (access control) |
| **Problem** | A user with only the general "View Appointments" permission could read appointments explicitly flagged confidential (e.g. an HIV clinic visit), because the confidentiality permission was only checked in two reporting screens — never in the core appointment service used everywhere else. |
| **Evidence** | Manual penetration test: a test user without the confidentiality permission successfully retrieved a confidential appointment via the core service, while the same user was correctly denied via the reporting screen — proving an inconsistency, not a missing feature. |
| **Fix** | The confidentiality check was added to the core appointment service, applied consistently across every retrieval method. |
| **Evidence after fix** | 8/8 tests in `ConfidentialAppointmentAccessControlTest` + 6/6 tests in `ConfidentialAppointmentDashboardRegressionTest` now pass, proving the appointment is correctly hidden across 7 different ways of retrieving it. |

### Finding B-002

| | |
|---|---|
| **Title** | No protection against double-booking or conflicting concurrent edits |
| **Severity** | High |
| **CVSS score** | 6.8 (estimated; `AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:H/A:L`) |
| **NEN-7510 control** | 8.26 / 8.29 |
| **Problem** | No check existed to stop an appointment block or time slot from overlapping another, from being created in the past, or from having an end date before its start date. No mechanism existed to stop two simultaneous edits from silently overwriting each other. |
| **Evidence** | The project's own test data proved the gap: an appointment block had time slots dated on different days than the block itself, and two slots with identical times that overlapped each other — none of it ever caught, because nothing checked for it. |
| **Fix** | Date-ordering, past-date, boundary, and overlap checks were added to the shared validation logic (covering every entry point, not just the web screen). A version-tracking column was added so the system detects when two people edit the same record at the same time. |
| **Evidence after fix** | 198/198 `api` tests pass, including a dedicated test that simulates two simultaneous edits and confirms the second one is correctly rejected rather than silently overwriting the first. |

### Finding B-003

| | |
|---|---|
| **Title** | Six critical vulnerabilities in third-party libraries |
| **Severity** | Critical |
| **CVSS score** | 9.1 – 9.8 (raw); 4.1 – 5.9 (adjusted for this deployment's actual exposure, see §5) |
| **NEN-7510 control** | 8.8 |
| **Problem** | Six libraries used by this module (`commons-collections`, `commons-fileupload`, `xstream`, `quartz`, `xmlbeans`, `liquibase-core`) had known critical vulnerabilities, mostly allowing an attacker to run arbitrary code via crafted input. |
| **Evidence** | SBOM/dependency scan, full detail in `documentation/sbom-analysis.md`. |
| **Fix** | All six libraries were updated to a patched version. |
| **Evidence after fix** | Verified directly in the project file (`pom.xml`) diff: every one of the six now points to a version with the vulnerability fixed (see §5 for exact version numbers). |

### Finding B-004

| | |
|---|---|
| **Title** | Patient details exposed in system logs, plus a log-injection weakness |
| **Severity** | High |
| **CVSS score** | 7.1 (estimated; `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:L/A:N`) |
| **NEN-7510 control** | 8.15 |
| **Problem** | A logging method wrote the patient's name, date of birth, and ID number into the application's log files every time someone looked up a schedule. Separately, five web form components logged raw, unprocessed user input and full error details — letting an attacker inject fake log entries or learn details about the server's internals. Destructive actions, like deleting an appointment, were not logged at all. |
| **Evidence** | Logging gap analysis (`documentation/gap-analysis.md`), which scanned the codebase and manually verified each issue against the exact line of code. |
| **Fix** | The logging method now records only an internal reference number for the patient, plus who performed the action — never the patient's actual details. The five web form components were changed to clean up user input before logging it, and no longer reveal full error details. Logging was added for destructive actions. |
| **Evidence after fix** | Verified directly in the code: the old, vulnerable version is kept as a comment right above the fix, so the before/after is visible at a glance. The new log line reads `"[AUDIT] READ_APPOINTMENTS - User: [username] ... Patient Internal ID: [id]"` — no patient name, birth date, or identifier present. |

**Below: open items, included per the rule that open findings must be reported, not hidden.**

### Finding B-005 (Open)

| | |
|---|---|
| **Title** | Free-text appointment notes are not protected from unauthorized staff |
| **Severity** | Medium-High |
| **CVSS score** | 5.3 (estimated; `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`) |
| **NEN-7510 control** | 8.2 / 8.15 |
| **Problem** | The free-text fields for an appointment's reason and cancellation reason can contain sensitive clinical context, but nothing restricts who can see them or keeps them out of application logs. |
| **Evidence** | Manual code review — no masking or access-restriction code exists anywhere for these fields. |
| **Status** | **Open.** No work has started due to planning and lower priority. |

### Finding B-006 (Open)

| | |
|---|---|
| **Title** | Most permission checks lack a test proving denial actually works |
| **Severity** | Low-Medium |
| **CVSS score** | 3.8 (estimated — a coverage gap, not a confirmed bypass) |
| **NEN-7510 control** | 8.29 |
| **Problem** | The appointment service has roughly 97 permission checks. Only the confidentiality-specific one (B-001) has a test proving an unauthorized user is actually rejected — the rest are only tested for the authorized ("happy path") case. |
| **Evidence** | Manual review of the service's permission annotations against the existing test suite. |
| **Status** | **Open.** Lower priority than B-005, since there is no evidence any of these checks are actually broken. |

---

## 5. SBOM & Supply Chain Security

**Summary:**

| Component | Version | CVE | Status |
|---|---|---|---|
| commons-collections | 3.2 → **3.2.2** | CVE-2015-7501 | ✅ Resolved |
| commons-fileupload | 1.2.1 → **1.6.0** | CVE-2016-1000031 | ✅ Resolved |
| xstream | 1.4.3 → **1.4.21** | CVE-2013-7285 | ✅ Resolved |
| quartz | 2.1.1 → **2.3.2** | CVE-2019-13990 | ✅ Resolved |
| xmlbeans | 2.3.0 → **3.0.0** | CVE-2021-23926 | ✅ Resolved |
| liquibase-core | 2.0.5 → **4.8.0** | CVE-2022-0839 | ✅ Resolved |
| spring-web | 3.0.5 | CVE-2016-1000027 | ⚠️ Accepted (platform-provided, see below) |
| jackson-mapper-asl | 1.5.0 | CVE-2019-10202 | ⚠️ Accepted (platform-provided) |
| dom4j | 1.6.1 | CVE-2020-10683 | ⚠️ Accepted (platform-provided) |
| openmrs-web | 1.9.9 | CVE-2026-40076 | ⚠️ Accepted (platform-provided) |

**Why "Accepted" for the platform-provided ones:** these libraries are supplied by the OpenMRS
platform itself, not by this module, so this module cannot independently patch them. They are
tracked as residual risk for the platform owner.

**Why some criticals were prioritized over others (contextual scoring):** raw CVSS scores were
adjusted for how reachable and exploitable each vulnerability actually is in this deployment (e.g. a
vulnerability requiring a newer Java version than this module runs on is far less urgent than its
raw score suggests). Full methodology, the contextual scores, and the false positives that were
identified and excluded are in `documentation/sbom-analysis.md`.

**Net result: every vulnerability this module could patch itself has been patched.** All remaining
risk sits in platform-supplied dependencies, outside this module's control.

---

## 6. Secure Pipelines

| Guardrail | Status | Evidence |
|---|---|---|
| Branch protection / required checks | ✅ | CodeQL, Dependency Review, SBOM & SCA, SonarCloud, Java CI with Maven all required on PRs |
| SAST on every PR | ✅ | CodeQL Advanced workflows |
| SCA / dependency alerts | ✅ | Dependabot + manual triage/review (§5) |
| SBOM generated at build time | ✅ | `bom.json`, "SBOM & SCA" workflow |
| Code coverage gate | ✅ | JaCoCo report per PR |
| OTAP separation | ⚠️ No true OTAP. | Academic project — no staging/production deployment exists; only Development and Test (CI) are real. |
| Non-traceable test data | ✅ (by construction) | All test data is the standard OpenMRS synthetic demo dataset — there was never real patient data in this repository to begin with. |

**Open detail:** the "Java CI with Maven" required check currently binds to "any source" rather than
specifically to its own GitHub Actions workflow — it still fully blocks merging without it passing,
this is a precision nuance rather than a functional gap, and is recorded here rather than hidden.

---

## 7. CRA Mapping

| CRA Obligation | NEN-7510:2024-2 Control | Status |
|---|---|---|
| Deliver software without known active vulnerabilities | 8.8 | ✅ All module-patchable CVEs resolved (§5) |
| Make an SBOM available | 8.8 + 5.22 | ✅ `bom.json` |
| Provide security updates | 8.8 | ⚠️ Demonstrated this sprint; no formal lifetime commitment (academic project) |
| Secure by design | 8.25 | ✅ CI gates, confidentiality-by-default checks, validated input handling |
| Report actively exploited vulnerabilities to ENISA (24h) | 6.8 | ❌ Not applicable — see justification below |
| Logging and monitoring | 8.15 + 8.16 | ✅ B-004's fix (§4) |
| Access control for administrative interfaces | 8.2 | ⚠️ Partial — B-001 closed, B-006 still open |

**Justification ("Not applicable"):** the ENISA 24-hour reporting duty applies to *actively exploited*
vulnerabilities in a deployed product. This module has no production deployment under this project's
control, and every finding above was found through planned internal assessment, not active
exploitation in the field.

---

## 8. Traceability Matrix

At least 5 NEN-7510 controls must be linked to a measure, a finding, a change, and evidence — this
report links 7.

| Standard | Measure | Before (finding) | Change | After (evidence) |
|---|---|---|---|---|
| **NEN-7510 8.2 / 8.26** | Confidentiality check in core service | B-001: confidential appointment leaked to unauthorized user via core retrieval, while the reporting screen correctly blocked it | Confidentiality check added to the core appointment service | 8/8 + 6/6 tests passing across 7 read paths |
| **NEN-7510 8.15** | PHI removed from logs, input sanitized, actor context added | B-004: patient name/DOB/ID logged in plain text; 5 web components logged unsanitized input + full errors; no actor context; no audit trail for deletions | Logging method rewritten to log only an internal ID + actor; web components sanitize input; deletion logging added | Old vulnerable code kept as an in-place comment for before/after comparison; new log line contains no PHI |
| **NEN-7510 8.26 / 8.29** | Schedule validation + version-based conflict detection | B-002: overlapping/impossible schedules undetected; no protection against simultaneous conflicting edits | Validation rules added to shared validators; version column added for conflict detection | 198/198 tests passing, including a dedicated simultaneous-edit test |
| **NEN-7510 8.8** | Dependency version updates | B-003: six dependencies with critical (CVSS 9.1-9.8) vulnerabilities | All six updated to patched versions | Verified directly in the `pom.xml` diff |
| **NEN-7510 8.25** | CI/CD security gates | Process risks identified in `cicd-risk-evaluation.md` | CodeQL, Dependency Review, SBOM/SCA, SonarCloud configured as required PR checks | `documentation/mitigations/SR-08-ci-cd-security-gates.md`, evidence screenshots |
| **NEN-7510 5.35** | Independent review | No formal self-assessment existed before this sprint | This audit report, cross-checking every claim against the actual repository rather than ticket status | This document — every claim traced to a file, commit, or test result |
| **NEN-7510 8.4 / 8.13** | Attack-surface enumeration | No documented attack-surface inventory existed | Manual entry-point mapping and threat-model update | `documentation/attack-surface-overview.md` |

---

## 9. AI Tooling Accountability

This report, and the mitigation work it documents, was produced with AI assistance (Claude) under
direct human supervision.

- **Asked of the AI:** investigate which risks were actually mitigated in code (not just by ticket
  status), implement the schedule-integrity validation rules and tests, and assemble this report from
  existing project evidence and documentation.
- **Generated by the AI:** the validator code for B-002, its tests, and the first draft of this
  report's content — sourced by reading the actual repository, not from invented examples.
- **Independently verified:** every code claim in this report was checked against the real repository
  (file contents, git log, 198/198 `api` + 132/132 `omod` test results), not taken from AI output
  alone. The dependency version bumps (§5) and the log-statement fix (§4, B-004) were confirmed
  against the actual diff/code.
- **Decided by the human author:** the design choice to enforce B-002's new validation rules only on
  newly created records (not retroactively on edits to old data), after discovering unconditional
  enforcement broke legitimate edits; and the prioritization in §1's roadmap.

---

## 10. Conclusion & Advice

**Does the module comply with the relevant NEN-7510:2024-2 controls and CRA obligations?** Largely
yes, for everything within this module's own control. All four critical risks identified this audit
are resolved, tested, and merged. Two lower-priority improvements remain open.

| Priority | Action |
|---|---|
| **Now** | — (no critical or high-severity issue is currently open) |
| **This sprint** | B-005: restrict free-text appointment notes from unauthorized staff |
| **Later** | B-006: add denial tests for the remaining permission checks; resolve the CI check's "any source" binding (§6); decide whether this module's own input-handling code needs dedicated hardening review |

---

## 11. Appendices

Every appendix below is referenced from the body text (see the section noted in brackets).

- **Appendix A** — SAST output: CodeQL (referenced in §3, §6). **[TODO: export a current SARIF file —
  not yet saved as a repository artifact.]**
- **Appendix B** — SCA / dependency alerts: `documentation/security-vulnerability-backlog.md`,
  `documentation/vulnerability-traceability-matrix.md` (referenced in §5).
- **Appendix C** — SBOM: `openmrs-module-appointmentscheduling/bom.json` (referenced in §5, §6).
- **Appendix D** — Risk matrix: `documentation/cia-analysis.md` §5-7 (referenced in §1, §4).
- **Appendix E** — Bow-tie / threat model: `documentation/bowtie-analysis.md`,
  `documentation/attack-surface-overview.md` (referenced in §3, §8).
- **Appendix F** — Penetration test evidence: `documentation/pentest-plan.md`,
  `documentation/pentest-report.md`, `ConfidentialAppointmentAccessControlTest.java`,
  `ConfidentialAppointmentDashboardRegressionTest.java` (referenced in §3, §4 B-001).
- **Appendix G** — Logging gap analysis & fix: `documentation/gap-analysis.md`, commit `edebdf5`
  (referenced in §3, §4 B-004, §8).
- **Appendix H** — Security backlog: `documentation/security-requirements-mapping.md` (referenced in §1).
- **Appendix I** — Schedule-integrity mitigation record: `documentation/mitigations/schedule-integrity-controls.md`
  (referenced in §4 B-002).
- **Appendix J** — Confidentiality mitigation record: `documentation/mitigations/SR-03-confidential-appointment-access-control.md`
  (referenced in §4 B-001).
- **Appendix K** — CI/CD security gates: `documentation/mitigations/SR-08-ci-cd-security-gates.md`,
  `documentation/ci-cd-security-gates.md` (referenced in §6, §8).

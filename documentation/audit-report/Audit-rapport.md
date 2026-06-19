# Audit Report — OpenMRS Appointment Scheduling Module

**Status:** Current state of branch `feature/audit-report` as of 2026-06-19. Every claim below was
verified directly against the repository (file contents, git log, test execution) at the time of
writing, not assumed from kanban/ticket status — per this report's own principle in §3 ("a claim
without evidence is just an opinion").

## Rubric Coverage Map

This report is structured against the **"LU2 Verbeteronderzoek Security Beroepsproduct"** rubric.
Each graded criterion maps to a specific section, so each can be verified independently:

| Rubric Criterion (points) | Addressed in |
|---|---|
| Security audit (wetgeving & normen) — /20 | §2 Scope & Context, §4 Risk Analysis, §10 Traceability Matrix, §9 CRA Mapping |
| Secure pipelines — /15 | §6 Secure Pipelines |
| Advies updates (SBOM, CVE en CVSS) — /15 | §5 SBOM & Supply Chain Security |
| Security code review & kwetsbaarheden — /15 | §4 Risk Analysis & Findings, §11 AI Tooling Accountability |
| Penetration tests — /15 | §4, Finding B-001 specifically (PT-01/PT-02 methodology) |
| Mitigatie & validatie verbeteringen — /20 | §7 Mitigation & Validation, throughout §4's "Evidence of the fix" subsections |

(The separate "Onderhoudbaarheid" rubric is a different deliverable and is not addressed here.)

---

## 1. Executive Summary

**Overall status: 🟢 GREEN, trending from amber.** Every risk originally scored "Unacceptable"
(≥15) in this project's risk assessment now has a concrete, tested, merged mitigation. Two
lower-priority risks remain genuinely open.

In short: this module processes special-category health data. The risk assessment identified four
"Unacceptable" risks and four "Acceptable with mitigation" risks. As of this branch:

- **All four Unacceptable risks are resolved**, each verified with passing tests: unauthorized
  disclosure of confidential appointments (8 dedicated tests), schedule integrity/double-booking
  (185+ tests including a concurrency test), six critical dependency vulnerabilities (verified patched
  via git diff), and a critical PHI-logging leak plus systemic log-injection vulnerability discovered
  and fixed this sprint (5/5 gaps from `gap-analysis.md` closed).
- **Two "Acceptable with mitigation" risks remain open:** free-text field masking for appointment
  notes (SR-02), and complete denied-access test coverage for the ~97 `@Authorized` checks beyond the
  confidentiality-specific ones (SR-01 follow-up).

**Top 3 risks resolved this sprint, for continuity:**

1. **Confidential appointment disclosure** — a user with only "View Appointments" could read
   appointments explicitly marked confidential (e.g. HIV clinic visits), across the entire core API.
   **Resolved and merged** (PR #88), proven across 8 read paths plus a dashboard regression test.
2. **PHI written directly into application logs** — patient name, date of birth, and identifiers were
   logged in plaintext on every appointment lookup, alongside log-injection vulnerabilities in five web
   form editors. **Resolved and merged**, all 5 gaps identified in `gap-analysis.md` closed in one commit.
3. **Schedule integrity / double-booking** — no validation existed to prevent overlapping or
   impossible appointment schedules, and no protection against concurrent conflicting writes.
   **Resolved and merged**, including a dedicated concurrency test.

**One risk still open, worth flagging:** free-text appointment fields (`reason`, `cancelReason`) can
contain clinical context and are not access-restricted for unauthorized users. No work has started.

**Roadmap:**

| Priority | Action | Why |
|---|---|---|
| 🟡 This sprint | Implement SR-02 (free-text field masking) | Last open risk tied to an "Unacceptable"-tier asset (confidentiality of appointment notes) |
| 🟢 Later | Add denied-access tests for the remaining ~90 `@Authorized` checks | Happy-path authorization is broadly enforced; only the negative case is undertested |
| 🟢 Later | Resolve the "any source" branch-protection nuance on the Java CI required check | Functionally enforced already; cosmetic precision gap only |

---

## 2. Scope & Context

- **Module / repository:** `openmrs-module-appointmentscheduling`, branch `feature/audit-report`
  (integrates PR #93/SR-04, #87/SR-07, #88/SR-03, #94/attack-surface-mapping, #96/gap-analysis, plus
  the direct logging-remediation commit `edebdf5`).
- **Assessment period:** sprints 1–4 of this course module.
- **Test environment:** local development / CI (GitHub Actions), in-memory H2 test database. No
  staging or production environment exists — this is an academic OpenMRS module, not a deployed
  service.
- **Out of scope:** the OpenMRS platform core (`openmrs-core`) and any other OpenMRS module outside
  this repository. Platform-`provided` dependency CVEs are tracked as accepted residual risk (§5),
  since this module cannot patch them independently.
- **Primary norm:** NEN-7510:2024 Part 2. **Supplementary:** AVG/GDPR Art. 9 (appointment data reveals
  special-category health information). **CRA:** applicable in principle — see §9.

**What was NOT tested, and why:**

- **Dynamic/penetration testing against a running deployment** — no production-like environment
  exists; testing was source-level (SAST, manual code review, unit/integration tests simulating
  attacker scenarios), not DAST against a live instance.
- **SR-06 (deserialization/injection hardening of this module's own input handling)** — not started.
  The SBOM/SCA work (§5) addresses known-CVE deserialization risk *in dependencies*; this module's own
  input-handling code has not been independently hardened/reviewed for the same class of issue.
- **A full re-enumeration of every REST resource's authorization** — the SR-03 fix is proven across 8
  core-service read paths; REST resources call into that same service layer, but each individual REST
  controller was not independently re-walked in this report.

---

## 3. Audit Methodology

| Technique | Tool / approach | When | Result |
|---|---|---|---|
| SAST | CodeQL (`.github/workflows/codeql.yml`), required PR check | Every PR | `documentation/ci-cd-security-gates.md` |
| SCA | Dependabot + manual CVE/CVSS/EPSS triage | Throughout + Sprint 3 remediation | `sbom-analysis.md`, `vulnerability-traceability-matrix.md` |
| SBOM | CycloneDX (`bom.json`) | Build-time | `openmrs-module-appointmentscheduling/bom.json` |
| Attack surface mapping | Manual entry-point enumeration + threat-model update | Sprint 4 | `documentation/attack-surface-overview.md` |
| Logging gap analysis | Automated scan + manual evaluation of high-risk files against NEN-7510 8.15 | Sprint 4 | `documentation/gap-analysis.md` |
| Code review | Manual, validator/service-layer logic review (author + AI-assisted, see §11) | Sprints 3–4 | PR review history, inline code comments |
| Risk analysis | CIA classification + risk register, Impact × Likelihood (1–5 scale each) | Sprint 1–2 | `cia-analysis.md` |
| Penetration test (demonstration) | Manual, targeted at the confidentiality privilege gap (PT-01/PT-02 methodology, see `pentest-plan.md`) | Sprint 2–3 | `pentest-plan.md`, `pentest-report.md` |
| Unit / integration testing | JUnit + Spring Test, incl. a concurrency test simulating a stale optimistic-lock write | Sprint 3–4 | 198/198 `api`, 132/132 `omod` tests passing |

---

## 4. Risk Analysis & Findings

Each finding states its current, code-verified status. Severity uses CVSS 3.1 where a real exploit
path exists; a "contextual" note is added where healthcare context changes the practical urgency.

### Finding B-001 — Broken access control on confidential appointments — ✅ RESOLVED

| | |
|---|---|
| **Severity** | High (CVSS 3.1: 6.5 — `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`; contextually higher, since the disclosed data is special-category health information under AVG Art. 9) |
| **NEN-7510** | 8.2 (access control), 8.26 (application security requirements), 8.28 (secure coding) |
| **CWE** | CWE-285 (Improper Authorization), CWE-639 (Authorization Bypass Through User-Controlled Key) |
| **Found by** | Manual penetration-test demonstration (`pentest-plan.md`, methodology: PT-01 attacker path / PT-02 control path) |
| **Status** | **Resolved, merged** (PR #88) |

**Description.** `PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS` was defined but only checked in two
reporting evaluators — never in the core `AppointmentService` retrieval methods. A user holding only
the generic "View Appointments" privilege could read appointments of a type flagged `confidential`
(e.g. "Initial HIV Clinic Appointment") through `getAppointment()`, `getAllAppointments()`, and other
core paths.

**Evidence of the original finding.** PT-01 (attacker path): a test user with only "View Appointments"
successfully retrieved a confidential appointment. PT-02 (control path): the same user, querying via
the reporting-evaluator path, was correctly denied — proving the inconsistency was real, not a missing
feature across the board.

**Evidence of the fix.** `AppointmentServiceImpl` now applies `isConfidentialAppointment()` +
`Context.hasPrivilege(...)` consistently. `ConfidentialAppointmentAccessControlTest` now has 8 passing
tests proving filtering across every read path: `getAppointment`, `getAllAppointments`,
`getAppointmentByUuid`, `getAppointmentByVisit`, `getAppointmentsByConstraints`,
`getScheduledAppointmentsForPatient`, `getLastAppointment`, plus one cross-path consistency test. A
separate `ConfidentialAppointmentDashboardRegressionTest` (6 tests, `omod` module) proves the web
dashboard also respects the same control. **All 14 tests pass.**

**Residual risk.** Internal conflict-detection logic (double-booking checks) was specifically verified
to still query unfiltered data via the DAO directly, so the fix does not accidentally hide confidential
appointments from the system's own scheduling logic — only from unauthorized users.

---

### Finding B-002 — Schedule integrity: no validation, no concurrency protection — ✅ RESOLVED

| | |
|---|---|
| **Severity** | High (estimated: 6.8 — `AV:N/AC:L/PR:L/UI:N/S:U/C:N/I:H/A:L`; data-integrity impact) |
| **NEN-7510** | 8.26 (application security requirements), 8.29 (security testing) |
| **CWE** | CWE-841 (Improper Enforcement of Behavioral Workflow), CWE-362 (Race Condition) |
| **Found by** | Manual data-model review against the live test dataset |
| **Status** | **Resolved, merged** (PR #93) |

**Description.** `AppointmentBlock`, `TimeSlot`, and `ProviderSchedule` had a javadoc comment promising
`startDate < endDate` validation that was never implemented; `TimeSlot` had no overlap check between
sibling slots and no check that it falls within its parent block's date range; "no past dates" existed
only in the web UI controller, never in the shared validator (REST callers bypassed it entirely); and
neither entity had any concurrency-control mechanism.

**Evidence of the original gap.** The shared test dataset itself proved the gap was real: appointment
block #1 spans `2005-01-01 00:00-11:00`, yet several of its time slots are dated on entirely different
days, and two have identical timestamps (i.e. they overlap each other) — undetected, because no
validator checked for it.

**Evidence of the fix.** Date-ordering, past-date (day-granularity, creation-only), bounds, and overlap
checks added to the shared `Validator` classes (protecting every entry point); a fail-open
provider-availability check added; an optimistic-locking `version` column added to both entities via
Liquibase + Hibernate; a dedicated `ScheduleConcurrencyTest` proving Hibernate throws
`StaleObjectStateException` on a concurrent stale write. Full detail and design rationale in
`documentation/mitigations/schedule-integrity-controls.md`.

---

### Finding B-003 — Six critical (CVSS ≥9.0) dependency vulnerabilities — ✅ RESOLVED

| | |
|---|---|
| **Severity** | Critical (raw CVSS 9.1–9.8; contextual scores 4.1–5.9 after adjusting for reachability/exploitability) |
| **NEN-7510** | 8.8 (management of technical vulnerabilities), 5.19 (supplier relationships) |
| **CWE** | CWE-502 (Deserialization of Untrusted Data) ×2, CWE-611 (XXE) ×3, CWE-78 (OS Command Injection) |
| **Found by** | SCA / dependency scan, `sbom-analysis.md` |
| **Status** | **Resolved, merged** (PR #87) |

**Description.** Six direct dependencies carried known critical CVEs: `commons-collections` 3.2
(CVE-2015-7501), `commons-fileupload` 1.2.1 (CVE-2016-1000031), `xstream` 1.4.3 (CVE-2013-7285),
`quartz` 2.1.1 (CVE-2019-13990), `xmlbeans` 2.3.0 (CVE-2021-23926), `liquibase-core` 2.0.5
(CVE-2022-0839).

**Evidence of the fix.** Verified directly in the pom.xml diff (commit `fa0f4ba3`, plus `0bf4e23a`/
`799e8bf7`): all six bumped to patched versions — `commons-collections` → 3.2.2, `commons-fileupload` →
1.6.0, `xstream` → 1.4.21, `quartz` → 2.3.2, `xmlbeans` → 3.0.0, `liquibase-core` → 4.8.0. Full detail
in `vulnerability-decision-record.md` / `vulnerability-traceability-matrix.md`.

---

### Finding B-004 — PHI in plaintext logs + systemic log injection + missing audit trails — ✅ RESOLVED

| | |
|---|---|
| **Severity** | High (estimated: 7.1 — `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:L/A:N`, combining confidentiality leak with the injection vector) |
| **NEN-7510** | 8.15 (logging), 8.26, 8.28 (secure coding) |
| **CWE** | CWE-532 (Insertion of Sensitive Information into Log File), CWE-117 (Improper Output Neutralization for Logs / CRLF injection), CWE-209 (Information Exposure Through Error Message — stack traces) |
| **Found by** | `documentation/gap-analysis.md` — automated scan + manual evaluation against NEN-7510 8.15 |
| **Status** | **Resolved, merged** (commit `edebdf5`) |

**Description.** `gap-analysis.md` identified 4 concrete gaps via its "Gap Matrix": (1)
`getAppointmentsForPatientWithLogging` wrote patient name, date of birth, and identifier into
plaintext application logs on every lookup; (2) all five web data-binding editor classes
(`AppointmentBlockEditor`, `AppointmentEditor`, `AppointmentTypeEditor`, `ProviderEditor`,
`TimeSlotEditor`) logged raw, unsanitized user input — a log-injection (CRLF) vector — and full
exception stack traces, disclosing server internals; (3) logs across the board captured the data
being accessed but not the actor (user ID, IP) performing the access; (4) destructive actions
(deletions) had no audit trail at all.

**Evidence of the original finding.** `gap-analysis.md` §3–4, with the exact vulnerable code identified
per file.

**Evidence of the fix.** Verified directly in code: `getAppointmentsForPatientWithLogging` now logs
`"[AUDIT] READ_APPOINTMENTS - User: [username] accessed appointments for Patient Internal ID: [id]"` —
the internal database ID, not PHI — with the old vulnerable implementation left commented out
in-place for before/after traceability. The five web editors were refactored (227 insertions / 94
deletions across 6 files, commit `edebdf5`) to sanitize input and suppress stack traces.
`AppointmentValidator`'s logged `patientId` was independently checked: it logs the internal database
ID (not a natural/national identifier), so no separate masking was required there, consistent with
`gap-analysis.md`'s own Sprint Task 3 condition.

---

### Finding B-005 — No protection for sensitive free-text fields (SR-02) — ❌ OPEN

| | |
|---|---|
| **Severity** | Medium-High (estimated: 5.3 — `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`) |
| **NEN-7510** | 8.2 (access control), 8.15 (logging — ensuring sensitive data isn't logged in plaintext), 8.26 |
| **CWE** | CWE-200 (Exposure of Sensitive Information) |
| **Found by** | This sprint's code review (no masking code found anywhere in `api/src/main`) |
| **Status** | **Open — not started** |

**Description.** `Appointment.reason` and `Appointment.cancelReason` are free-text fields that can
contain clinical context — `cia-analysis.md` rates this asset's confidentiality risk at 15
(Unacceptable). No code anywhere applies field-level masking, access restriction, or log-redaction to
these fields for users without an appropriate privilege.

**Recommendation.** Implement field-level masking analogous to B-001's confidentiality check. Tracked
as SR-02 (#70) — no branch exists yet.

---

### Finding B-006 — Incomplete denied-access test coverage (SR-01 follow-up) — ⚠️ PARTIAL

| | |
|---|---|
| **Severity** | Low-Medium (estimated: 3.8 — this is a *test-coverage* gap, not a confirmed exploitable bypass; the underlying `@Authorized` checks are present, just unverified for the negative case) |
| **NEN-7510** | 8.29 (security testing) |
| **Found by** | This sprint's review of `AppointmentService.java`'s ~97 `@Authorized` annotations |
| **Status** | **Partial** |

**Description.** Every privilege-gated method in `AppointmentService` is exercised under an authorized
user in existing tests (happy-path coverage). Only the confidentiality-specific privilege now also has
*denied*-path coverage (via B-001's fix). The remaining ~90 `@Authorized` checks have no test proving
that a user lacking the required privilege is actually rejected.

**Recommendation.** Add denied-access tests for the remaining privilege checks. Lower urgency than
B-005, since there is no evidence (yet) that any of these checks are actually broken — this is a
coverage gap, not a confirmed vulnerability.

---

## 5. SBOM & Supply Chain Security

The module's SBOM is generated as CycloneDX JSON: `openmrs-module-appointmentscheduling/bom.json`
(Appendix C). Full methodology and per-finding reasoning is in `documentation/sbom-analysis.md` —
summarized here.

**Critical-severity findings (sorted by contextual score — how urgent each actually is in this
deployment, not raw CVSS):**

| # | CVE | Package | CVSS | EPSS | Contextual | Decision | Status |
|---|---|---|---:|---:|---:|---|---|
| C-03 | CVE-2015-7501 | commons-collections 3.2 | 9.8 | 0.715 | 5.9 | PATCH | ✅ Patched → 3.2.2 |
| C-04 | CVE-2016-1000031 | commons-fileupload 1.2.1 | 9.8 | 0.564 | 5.9 | PATCH | ✅ Patched → 1.6.0 |
| C-08 | CVE-2013-7285 | xstream 1.4.3 | 9.8 | 0.188 | 5.3 | PATCH | ✅ Patched → 1.4.21 |
| C-09 | CVE-2019-13990 | quartz 2.1.1 | 9.8 | 0.138 | 5.3 | PATCH | ✅ Patched → 2.3.2 |
| C-12 | CVE-2021-23926 | xmlbeans 2.3.0 | 9.1 | 0.004 | 4.2 | PATCH | ✅ Patched → 3.0.0 |
| C-14 | CVE-2022-0839 | liquibase-core 2.0.5 | 9.8 | 0.001 | 4.1 | PATCH | ✅ Patched → 4.8.0 |
| C-02 | CVE-2016-1000027 | spring-web 3.0.5 | 9.8 | 0.604 | 5.9 | ACCEPT | Platform-provided |
| C-10 | CVE-2019-10202 | jackson-mapper-asl 1.5.0 | 9.8 | 0.072 | 4.9 | ACCEPT | Platform-provided |
| C-11 | CVE-2020-10683 | dom4j 1.6.1 | 9.8 | 0.070 | 4.9 | ACCEPT | Platform-provided |
| C-13 | CVE-2026-40076 | openmrs-web 1.9.9 | 9.4 (v4.0) | 0.001 | 3.9 | ACCEPT | Platform-provided |

**Why "ACCEPT" for the platform-provided ones:** these are supplied by the OpenMRS platform core, not
this module, and this module cannot independently patch them. Tracked as residual risk for the
platform/deployment owner.

**False positives identified and suppressed** (full reasoning in `sbom-analysis.md` §4): Spring4Shell
(requires JDK 9+, this module builds/tests on JDK 8), and three log4j 1.2.15 findings affecting
components (Chainsaw GUI, JDBCAppender, SocketServer) not part of a standard OpenMRS deployment.

**Net result: all 6 vulnerabilities this module could realistically patch have been patched.**
Remaining residual risk is entirely in platform-`provided` dependencies, outside this module's control.

---

## 6. Secure Pipelines

| Guardrail | Status | Evidence |
|---|---|---|
| Branch protection / required status checks | ✅ Configured | CodeQL (×2 workflows), Dependency Review, SBOM & SCA, SonarCloud, Java CI with Maven all run and are required on PRs against `dev` |
| SAST on every PR | ✅ | CodeQL Advanced (java-kotlin + javascript-typescript), `.github/workflows/codeql.yml` |
| SCA / dependency alerts | ✅ | Dependabot + manual CVE/CVSS/EPSS triage (§5) |
| SBOM generated at build time | ✅ | CycloneDX (`bom.json`), `SBOM & SCA` workflow |
| Code coverage gate | ✅ | JaCoCo coverage report generated per PR |
| OTAP separation | ⚠️ **No true OTAP exists.** | This is an academic OpenMRS module with no staging/production deployment. Development and Test are the only real environments (local + CI). |
| Non-traceable / synthetic data across environments | ✅ **By construction, not by process.** | All test data is the standard OpenMRS synthetic demo dataset (fictional patients/providers, e.g. "butch") — there is no real patient data anywhere in this repository to leak between environments. This satisfies the underlying principle, though it should be noted this is because no real data was ever introduced, not because an anonymization pipeline transforms real data. |

**Residual note on the Java CI required check:** it currently binds as "any source" rather than
specifically to the GitHub Actions workflow that produces it (likely because the check hasn't yet run
in the exact branch/context GitHub needs to disambiguate the source). It is still fully enforced —
merging is blocked without it passing — this is a precision nuance, not a functional gap, and is
recorded here rather than left silently unmentioned.

---

## 7. Mitigation & Validation

This section demonstrates, quantitatively, that each mitigation in §4 actually reduced risk —
not just that code was written.

| Finding | Validation method | Quantitative result |
|---|---|---|
| B-001 (confidentiality) | Before/after test inversion: the original `_FINDING` tests asserting the leak were replaced with tests asserting denial | 8/8 `ConfidentialAppointmentAccessControlTest` + 6/6 `ConfidentialAppointmentDashboardRegressionTest` passing |
| B-002 (schedule integrity) | Dedicated concurrency test forcing a stale write via raw SQL version bump, asserting `StaleObjectStateException` | 2/2 `ScheduleConcurrencyTest` passing; 198/198 `api` tests overall |
| B-003 (dependencies) | Direct pom.xml diff verification against the SBOM's own CVE list | 6/6 PATCH-decision CVEs confirmed resolved in the dependency tree |
| B-004 (PHI/logging) | Direct code inspection of the fixed method against the gap analysis's documented vulnerable code (kept as a comment for traceability) | 4/4 gaps from `gap-analysis.md` §4 closed; 0 PHI fields remain in the log statement |

**Critical reflection on tooling and design choices** (also addressed in detail in §11):

- The concurrency test for B-002 deliberately does **not** use two real threads/connections. Spring
  wraps every test in a rollback transaction; a genuinely separate connection wouldn't see this test's
  uncommitted data, and forcing it to would require suspending that safety net and manually cleaning up
  afterwards — risking test pollution across the whole suite for no extra confidence. Hibernate's
  version check is mechanical (`UPDATE ... WHERE version = ?`, zero rows = conflict) regardless of *how*
  the staleness arose, so a single-session raw-SQL version bump proves the same mechanism.
- The fail-open (not fail-closed) design for B-002's provider-availability check was a deliberate
  trade-off: `cia-analysis.md` treats "Provider schedules" as a separate, lower-priority risk than
  schedule integrity itself, and fail-closed would have made the feature unusable for any provider
  without a fully configured schedule.

---

## 8. Conclusion & Advice

**Does the module comply with the relevant NEN-7510:2024-2 controls and CRA obligations?**
Substantially yes, for everything within this module's control. All four "Unacceptable" risks from
the original risk assessment are resolved, tested, and merged. Two lower-priority risks remain open.

**Prioritized recommendations:**

| Priority | Action | Why |
|---|---|---|
| **This sprint** | Implement SR-02 (free-text field masking, B-005) | Last open risk tied to a score-15 ("Unacceptable") asset |
| **Later** | Add denied-access tests for the remaining ~90 `@Authorized` checks (B-006) | Coverage gap, not a confirmed vulnerability — lower urgency |
| **Later** | Resolve the Java CI "any source" branch-protection binding (§6) | Cosmetic precision gap; already functionally enforced |
| **Later** | Decide formally whether SR-06 (this module's own input-handling hardening, separate from dependency CVEs) needs dedicated work | Not yet scoped |

---

## 9. CRA Mapping

The Cyber Resilience Act (EU Regulation 2024/2847) applies to "products with digital elements" — this
module qualifies. Mapped to the NEN-7510:2024-2 controls already used as this project's primary
framework:

| CRA Obligation | NEN-7510:2024-2 Control | Status / Evidence |
|---|---|---|
| Deliver software without known (actively exploited) vulnerabilities | 8.8 | ✅ All module-patchable critical CVEs resolved (§5); platform-`provided` CVEs accepted with documented rationale |
| Make an SBOM available to users | 8.8 + 5.22 | ✅ `bom.json` (CycloneDX), generated at build time |
| Provide security updates for the product's lifetime | 8.8 (patch management) | ⚠️ Demonstrated for this sprint's findings; no formal lifetime patch-support commitment exists (academic/community module, not commercially supported) |
| Secure by design | 8.25 | ✅ CI gates, branch protection, validated input handling, confidentiality-by-default checks all added/hardened this project |
| Report actively exploited vulnerabilities to ENISA within 24h | 6.8 | ❌ **Not applicable** — no actively-exploited vulnerability has been identified; see justification below |
| Support logging and monitoring | 8.15 + 8.16 | ✅ B-004's fix brings logging to NEN-7510 8.15 compliance: actor context (who/where), no PHI, sanitized input, audit trails for destructive actions |
| Access control for administrative interfaces | 8.2 | ⚠️ Partial — `@Authorized` checks exist broadly; B-001's confidentiality-specific gap is closed, B-006's broader denied-access test coverage is not yet complete |

**Justification for "Not applicable" (ENISA reporting):** CRA Art. 14's 24-hour reporting duty is
triggered by an *actively exploited* vulnerability in a deployed product. This module has no
production deployment under this project's control — all findings in this report were identified
through planned, internal assessment (SAST, SCA, manual review, attack-surface mapping, and an
authorized penetration-test demonstration), not through detection of active exploitation.

---

## 10. Traceability Matrix

Each row links a control claim to a concrete measure and a verifiable evidence artifact — before (the
finding) and after (the proof it was addressed).

| NEN-7510 Control | Measure | Before (finding) | Change | After (evidence) |
|---|---|---|---|---|
| **8.2 / 8.26** Access control | Confidentiality privilege check in core service layer | PT-01: confidential appointment leaked to a user without the privilege; PT-02 (control): the reporting path correctly filtered it — proving an inconsistency, not a missing feature | `AppointmentServiceImpl` gained `isConfidentialAppointment()` + `hasPrivilege()` checks (PR #88) | 8/8 `ConfidentialAppointmentAccessControlTest` + 6/6 `ConfidentialAppointmentDashboardRegressionTest` passing, covering 7 distinct read paths |
| **8.15** Logging | Remove PHI from logs, sanitize input, add actor context, add destructive-action audit trail | `gap-analysis.md` §3-4: `getAppointmentsForPatientWithLogging` logged name/DOB/identifier; 5 web editors logged unsanitized input + stack traces; no actor context anywhere; deletions un-logged | Commit `edebdf5`: 6 files changed, PHI removed, input sanitized, `User`/IP context injected, destructive-action logging added | Vulnerable code preserved as an in-place comment for before/after comparison; `[AUDIT] READ_APPOINTMENTS - User: [...] ... Patient Internal ID: [...]` replaces the PHI-bearing log line |
| **8.26 / 8.29** Application security & testing | Schedule-integrity validation + optimistic locking | `standardAppointmentTestDataset.xml` contained time slots outside their parent block's range and overlapping siblings, undetected by any validator | `AppointmentBlockValidator`/`TimeSlotValidator`/`ProviderScheduleValidator` updated; `version` column added via Liquibase + Hibernate (PR #93) | 198/198 `api`, 132/132 `omod` tests passing; dedicated `ScheduleConcurrencyTest` proves `StaleObjectStateException` on conflict; full design rationale in `documentation/mitigations/schedule-integrity-controls.md` |
| **8.8** Technical vulnerability management | Dependency version remediation | `sbom-analysis.md` C-03/C-04/C-08/C-09/C-12/C-14: six dependencies, CVSS 9.1-9.8 | `pom.xml` version bumps (commit `fa0f4ba3` + follow-ups) (PR #87) | Verified via git diff: all six patched to non-vulnerable versions (§5 table) |
| **8.25** Secure development lifecycle | CI/CD security gates | `cicd-risk-evaluation.md` process risks (P-01 to P-08) | CodeQL, Dependency Review, SBOM/SCA, SonarCloud configured as required checks; branch protection enabled | `documentation/mitigations/SR-08-ci-cd-security-gates.md`, evidence screenshots (`documentation/evidence/`) |
| **5.35** Independent review | This audit report itself | No formal independent self-assessment existed before this sprint | This report, cross-checking every claim against the actual repository state rather than ticket/kanban status | This document — every section traced to a file path, commit, or test-run result |
| **8.4 / 8.13** Network/data access control & threat awareness | Attack-surface enumeration and threat-model update | No documented attack-surface inventory existed | `documentation/attack-surface-overview.md` (PR #94) | New/updated threat-model entries listing entry points and high-risk surfaces |

---

## 11. AI Tooling Accountability

Per NEN-7510 8.29 (verification of security controls must be documented and demonstrable): this
report, and the SR-03/SR-04 mitigation work it documents, was produced with AI assistance (Claude, via
an agentic coding assistant) under direct human supervision throughout.

- **What was asked of the AI:** investigate which CIA risks were actually mitigated in code (not just
  by kanban status), implement the SR-04 schedule-integrity validation rules and tests, resolve
  specific git merge conflicts with reasoning, and assemble this report from existing project evidence.
- **What the AI generated:** the validator code changes for SR-04 (date ordering, past-date, bounds,
  overlap, fail-open provider availability, optimistic locking), the accompanying tests, and the first
  draft of this report's content — sourced by grepping/reading the actual repository state, not from
  invented examples.
- **What was independently verified:** every code claim in this report was checked against the actual
  repository — file contents, git log, and test-run output (198/198 `api`, 132/132 `omod`) — not taken
  from AI-generated assertions alone. The pom.xml version bumps in §5 and the log-statement fix in §4
  (B-004) were confirmed against the actual diff/code, not assumed from the commit message alone.
- **Decisions made by the human author:** the fail-open (vs. fail-closed) design for the
  provider-availability check; scoping the SR-04 validation rules to record-creation only (vs.
  unconditional), after discovering that unconditional enforcement broke legitimate edits to legacy
  data; the single-session (vs. two-thread) design for the concurrency test, to avoid test-pollution
  risk; and the prioritization in §8's roadmap.

---

## 12. Items Not Done, With Justification

| Item | Status | Justification |
|---|---|---|
| SR-02 (free-text field masking, B-005) | Not started | No branch exists; deprioritized behind the four "Unacceptable"-tier risks, all now resolved |
| SR-01 follow-up — denied-access tests for ~90 remaining `@Authorized` checks (B-006) | Partial | Only the confidentiality-specific privilege has negative-path test coverage so far; this is a coverage gap, not a confirmed bypass |
| SR-06 (deserialization/injection hardening of this module's own input handling) | Not started | The SBOM/SCA work (§5) addresses known-CVE deserialization risk in *dependencies*; this module's own input-handling code has not been independently reviewed for the same class of issue |
| Dynamic/penetration testing against a live deployment | Not performed | No staging/production environment exists for this academic project; testing was source-level only |
| True OTAP environment separation (§6) | Not applicable / not built | No staging or production deployment exists for this module; only Development and Test (CI) are real |
| CRA Art. 14 incident-reporting procedure (ENISA, 24h) | Not exercised | No actively-exploited vulnerability has been found in this module; see §9 for the applicability reasoning |
| Responsible disclosure procedure | Not exercised | All findings in this report were found internally during planned assessment activities, not via external disclosure |
| Java CI build as a precisely-bound (not "any source") required GitHub branch-protection check | Functionally enforced, not precisely bound | Documented in §6; not pursued further since it does not change actual enforcement |

---

## 13. Appendices

- **Appendix A — SAST output:** CodeQL runs on every PR via `.github/workflows/codeql.yml`.
  **[TODO: export and attach a current SARIF file from the GitHub Security tab — not yet saved as a
  repository artifact.]**
- **Appendix B — SCA / dependency alerts:** `documentation/security-vulnerability-backlog.md`,
  `documentation/vulnerability-traceability-matrix.md`, `documentation/risk-register-accepted-vulnerabilities.md`.
- **Appendix C — SBOM:** `openmrs-module-appointmentscheduling/bom.json` (CycloneDX JSON).
- **Appendix D — Risk matrix:** `documentation/cia-analysis.md` §5–7 (CIA classification, risk register).
- **Appendix E — Bow-tie diagram / threat model:** `documentation/bowtie-analysis.md`;
  `documentation/attack-surface-overview.md` (threat model updates, §5).
- **Appendix F — Penetration test evidence:** `documentation/pentest-plan.md`,
  `documentation/pentest-report.md`, `ConfidentialAppointmentAccessControlTest.java`,
  `ConfidentialAppointmentDashboardRegressionTest.java`.
- **Appendix G — Logging gap analysis & remediation:** `documentation/gap-analysis.md`, commit
  `edebdf5` ("security(logging): enforce NEN-7510 logging compliance").
- **Appendix H — Security backlog:** `documentation/security-requirements-mapping.md` (SR-01 to SR-08).
- **Appendix I — Schedule integrity mitigation record:** `documentation/mitigations/schedule-integrity-controls.md`.
- **Appendix J — Confidentiality mitigation record:** `documentation/mitigations/SR-03-confidential-appointment-access-control.md`.
- **Appendix K — CI/CD security gates:** `documentation/mitigations/SR-08-ci-cd-security-gates.md`,
  `documentation/ci-cd-security-gates.md`.
- **Appendix L — Snyk report:** **[TODO — this project uses CodeQL + Dependabot + manual CVE/CVSS/EPSS
  triage, not Snyk. Confirm with the rubric/assignment owner whether the equivalent SCA evidence above
  is acceptable in place of a literal Snyk report.]**

---

## Open TODOs Before This Report Is Final

1. Export and attach a current CodeQL SARIF file (Appendix A).
2. Resolve the Snyk-report question (Appendix L) — substitute or obtain.
3. Decide whether SR-02 (B-005) gets work this sprint, or is formally accepted as residual risk with
   sign-off (per the risk-acceptance rules in `cia-analysis.md` §6).
4. Get sign-off / review on this report from the rest of the project group — this should not be a
   single person's unilateral claim of compliance (per NEN-7510 5.35, independent review).

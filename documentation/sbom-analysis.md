# SBOM Analysis - OpenMRS Appointment Scheduling Module

**NEN-7510:2024-2 controls addressed:** 8.8 (Management of technical vulnerabilities), 8.28 (Secure coding), 8.29 (Security testing in development and acceptance), 5.19 (Information security in supplier relationships)

**Input:** `openmrs-module-appointmentscheduling/bom.json` (CycloneDX 1.4, 111 components) and `sca-report/grype-report.json` (Grype 0.114.0)

**Scan date:** 2026-06-08

---

## How to Read This Document

A scanner reports *every* known vulnerability in our dependencies (135 in total). That raw list is not actionable on its own: you cannot fix 135 items, and many do not actually pose a risk in our specific deployment. This document turns that raw output into a short, ranked, justified list by answering three questions for each relevant finding:

1. **How severe is it for us?** - not just the generic CVSS score, but a *contextual* score adjusted for our real deployment (see §3).
2. **Is it a real risk in our case?** - false positives are identified and removed with written rationale (see §4).
3. **What should we do?** - every active finding ends with a decision: **patch**, **suppress**, or **accept** (see §5).

### Key concept: `compile` vs `provided` scope

This distinction (explained in §2) drives almost every decision below:

- **`compile`** - the library is bundled inside this module's own artifact. We control its version and can upgrade it ourselves.
- **`provided`** - the library is supplied by the OpenMRS *platform* at runtime, not shipped by this module. Upgrading usually requires coordination with the platform. **All 15 Critical findings are `provided` scope.**

### Section guide

| Section | What it contains |
|---------|------------------|
| §1-2 | Scope, source files, and the `compile` vs `provided` classification |
| §3 | The contextual scoring method (the formula and how each factor is decided) |
| §4 | False positives - findings excluded with written rationale |
| §5 | The active Critical findings: a summary table plus a detailed "card" per finding (verified CVSS, CWE, EPSS, CISA KEV, contextual score, decision) |
| §6-7 | High / Medium / Low findings, grouped by package (full per-finding analysis deferred to sprint 3) |
| §8 | NEN-7510:2024-2 control mapping |
| §9 | Pre-packaged summaries for the downstream issues (#31 risk matrix, #35 backlog, #37 RAR) so they can consume this without redoing the analysis |

### The headline decisions

After removing 4 false positives, **10 active Critical findings** remain:

- **PATCH** (a fix exists) - commons-collections, commons-fileupload, xstream, quartz, xmlbeans, liquibase-core
- **ACCEPT** (no compatible fix / EOL library - flagged for the risk register in #37) - spring-web, jackson-mapper-asl, dom4j, openmrs-web
- **SUPPRESS** (false positives, see §4) - Spring4Shell (mitigated by JDK 8) and three log4j findings that require non-default configuration

> **Note on AI tooling (NEN-7510 8.29):** the initial triage and structuring of this document was AI-assisted; all CVSS scores, CWE categories, EPSS values, and CISA KEV status were verified manually against NVD and the GitHub Advisory Database. AI output is not used as evidence of a finding - it is a starting point that was independently checked.

---

## 1. Scope and Methodology

This document transforms the raw Grype SCA output from [issue #16](../../../issues/16) into a verified, scored, and classified findings list for use by:

- **#31** (risk matrix) - contextual scores and severity tiers as input
- **#35** (security backlog) - patch/accept decisions as prioritisation input
- **#37** (RAR) - accepted risks flagged for formal risk register

### Source material

| File | Purpose |
|------|---------|
| `sca-report/grype-report.json` | Raw scanner output: 15 Critical, 61 High, 54 Medium, 5 Low (135 total) |
| `openmrs-module-appointmentscheduling/bom.json` | SBOM for scope cross-reference (compile vs provided) |

### Analysis scope for this sprint

Sprint 2 covers analysis only. No code changes are made. The output of this document feeds sprint 3 planning.

---

## 2. Dependency Scope Classification

Maven scope determines whether a dependency is **bundled in this module's artifact** or **provided by the OpenMRS host platform**. This distinction directly affects reachability.

| Scope | Meaning | Examples from SBOM |
|-------|---------|-------------------|
| `compile` | Bundled in the module WAR - this module controls the version | joda-time 2.2 |
| `provided` | Not bundled - supplied by the OpenMRS platform at runtime | spring-*, log4j, hibernate-core, commons-*, xstream, quartz, jackson-mapper-asl, dom4j, xmlbeans, liquibase-core, openmrs-web, mysql-connector-java |

**Key implication:** All 15 Critical CVEs are in `provided` dependencies. This module does not bundle these libraries. However, the server it deploys on does contain them, so they remain in scope for the deployment risk assessment.

---

## 3. Contextual Scoring Methodology

### Formula

```
Contextueel = CVSS × (bereikbaarheid × 0.4 + healthcare-impact × 0.4 + exploit × 0.2)
```

### Factor definitions

**bereikbaarheid (reachability)** - how directly the vulnerability is reachable through this module in the target deployment:

| Value | Criteria |
|-------|---------|
| 0.1 | Exploit path technically blocked (e.g. wrong JDK version) |
| 0.2 | Requires specific non-default configuration (e.g. JDBCAppender) or high-privilege auth |
| 0.3 | Provided scope, hospital intranet deployment, not directly internet-facing |
| 0.5 | Compile scope, intranet deployment |
| 0.8 | Compile scope, internet-facing |
| 1.0 | Directly reachable, no preconditions |

**healthcare-impact** - severity of potential harm in the healthcare appointment scheduling context. The value is taken from the formal CIA (BIV) classification in [issue #29](cia-analysis.md), which rates the module's crown-jewel data (appointment records) as:

- **Confidentiality: critical (5)** - appointment records, requests, notes and confidential appointment types are special-category health data under AVG (GDPR) art. 9.
- **Integrity: critical (5)** - corrupted or manipulated scheduling data can delay or misdirect patient care.
- **Availability: high (4)** - loss of the scheduling service disrupts clinic operations, though it is not immediately life-critical.

Fixed value: **0.8** for all findings in this system. A C=5 / I=5 / A=4 classification - confidentiality and integrity critical, availability high - maps to a high (0.8) healthcare-impact on the 0-1 scale. It is not set to the maximum (1.0) because this is an appointment-scheduling module, so patient-safety impact is indirect (via delayed or misdirected care) rather than direct (as in a dosing or treatment module).

> **Reconciled with issue #29.** This factor was previously an interim value; it is now confirmed against the finalised CIA classification in #29. #29 rates availability slightly higher than the earlier interim estimate (high rather than medium), which reinforces - and does not lower - the 0.8 value, so the contextual scores in §5 are unchanged.

**exploit** - known exploitation activity based on EPSS and CISA KEV:

| Value | Criteria |
|-------|---------|
| 1.0 | Listed in CISA KEV catalog |
| 0.8 | EPSS ≥ 0.50 (high exploitation probability) |
| 0.5 | EPSS 0.10-0.49 |
| 0.3 | EPSS 0.01-0.09 |
| 0.1 | EPSS < 0.01 |

---

## 4. False Positive Analysis

The following findings are excluded from the active findings list with written rationale. They remain in the full Grype output but should not drive remediation effort.

| # | CVE | Package | Rationale | Decision |
|---|-----|---------|-----------|---------|
| FP-01 | CVE-2022-22965 (Spring4Shell) | spring-beans 3.0.5, spring-webmvc 3.0.5 | Spring4Shell requires JDK 9+ to exploit. OpenMRS 1.9.9 is built against Java 1.6 source/target and is tested with JDK 8 in CI (see `.github/workflows/sbom.yml`). The ClassLoader-based gadget chain that Spring4Shell uses does not exist in JDK 8's module system. The scanner flags this based on the Spring version alone without checking runtime JDK. | **SUPPRESS** - scanner artifact, JDK 8 deployment fully mitigates |
| FP-02 | CVE-2022-23307 | log4j 1.2.15 | Affects Apache Chainsaw, a standalone GUI log viewer application, not the server-side log4j library used in OpenMRS. Chainsaw is not deployed as part of the OpenMRS server runtime. | **SUPPRESS** - Chainsaw is not a server component |
| FP-03 | CVE-2022-23305 | log4j 1.2.15 | Affects the JDBCAppender, which requires explicit configuration to write logs to a JDBC database. Standard OpenMRS deployment uses file/console appenders, not JDBCAppender. Exploitation requires attacker-controlled log input reaching an active JDBCAppender. | **SUPPRESS** - JDBCAppender not configured in standard OpenMRS deployment |
| FP-04 | CVE-2019-17571 | log4j 1.2.15 | Affects the SocketServer class, which must be started separately as a daemon to receive log events from remote clients. It is not started as part of the OpenMRS web application. Exploitation requires the SocketServer to be running and listening on an accessible port. | **SUPPRESS** - SocketServer not started in standard OpenMRS deployment |

**Note:** FP-02, FP-03, FP-04 are conditional suppressions. If the deployment deviates from standard configuration (Chainsaw installed, JDBCAppender enabled, SocketServer started), these should be re-elevated to active findings.

---

## 5. Critical CVE Findings

### 5.1 Summary table

The scanner produced **15 Critical findings covering 14 unique CVEs** (Spring4Shell matches two Spring artifacts - spring-beans and spring-webmvc - so it counts as two findings but one CVE). After removing the 4 false positives documented in §4, **10 active Critical findings** remain.

Both tables below are sorted by **contextual score, highest first** - that is, by how urgent each finding actually is in our deployment, not by its raw CVSS. The "Decision" column says what we do about each one. Full per-finding reasoning is in §5.2.

**Active findings** (require a decision and follow-up):

| # | CVE | Package | CVSS | CWE | EPSS | KEV | Context | Decision |
|---|-----|---------|------|-----|------|-----|---------|---------|
| C-02 | CVE-2016-1000027 | spring-web 3.0.5 | 9.8 | CWE-502 | 0.604 | No | **5.9** | ACCEPT |
| C-03 | CVE-2015-7501 | commons-collections 3.2 | 9.8 | CWE-502 | 0.715 | No | **5.9** | PATCH |
| C-04 | CVE-2016-1000031 | commons-fileupload 1.2.1 | 9.8 | CWE-502 | 0.564 | No | **5.9** | PATCH |
| C-08 | CVE-2013-7285 | xstream 1.4.3 | 9.8 | CWE-78 | 0.188 | No | **5.3** | PATCH |
| C-09 | CVE-2019-13990 | quartz 2.1.1 | 9.8 | CWE-611 | 0.138 | No | **5.3** | PATCH |
| C-10 | CVE-2019-10202 | jackson-mapper-asl 1.5.0 | 9.8 | CWE-502 | 0.072 | No | **4.9** | ACCEPT |
| C-11 | CVE-2020-10683 | dom4j 1.6.1 | 9.8 | CWE-611 | 0.070 | No | **4.9** | ACCEPT |
| C-12 | CVE-2021-23926 | xmlbeans 2.3.0 | 9.1 | CWE-776 | 0.004 | No | **4.2** | PATCH |
| C-14 | CVE-2022-0839 | liquibase-core 2.0.5 | 9.8 | CWE-611 | 0.001 | No | **4.1** | PATCH |
| C-13 | CVE-2026-40076 | openmrs-web 1.9.9 | 9.4* | CWE-22 | 0.001 | No | **3.9** | ACCEPT |

**Suppressed findings** (false positives - full rationale in §4, not carried forward):

| # | CVE | Package | CVSS | Why suppressed |
|---|-----|---------|------|----------------|
| C-01 | CVE-2022-22965 (Spring4Shell) | spring-beans / spring-webmvc 3.0.5 | 9.8 | Exploit needs JDK 9+; we deploy on JDK 8 (FP-01) |
| C-05 | CVE-2019-17571 | log4j 1.2.15 | 9.8 | SocketServer daemon not started (FP-04) |
| C-06 | CVE-2022-23305 | log4j 1.2.15 | 9.8 | JDBCAppender not configured (FP-03) |
| C-07 | CVE-2022-23307 | log4j 1.2.15 | 9.8 | Chainsaw component not deployed (FP-02) |

\* C-13 uses CVSS v4.0 (vector `CVSS:4.0/AV:N/AC:L/AT:N/PR:H/UI:N`). All other scores are CVSS v3.1. EPSS values are the raw probability; percentiles are shown in the per-finding cards in §5.2.

---

### 5.2 Per-finding detail

Each active finding has a card below. To read a card: the top rows describe the vulnerability (package, scope, NVD-verified CVSS + vector, CWE root cause, EPSS, CISA KEV, whether a fix exists). The three rows `bereikbaarheid`, `healthcare-impact` and `exploit` are the scoring factors from §3, each with the reason for its value. The **Contextual score** row then applies the §3 formula:

`Contextual = CVSS × (bereikbaarheid × 0.4 + healthcare-impact × 0.4 + exploit × 0.2)`

So a raw CVSS of 9.8 with low reachability (0.3) comes out at **5.9** - still serious, but reflecting that it is not directly reachable in our intranet deployment. The final **Decision** row gives the action (patch / accept) and the reasoning behind it.

---

#### C-02 - CVE-2016-1000027 - spring-web

| Field | Value |
|-------|-------|
| Package | org.springframework:spring-web 3.0.5.RELEASE |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-502 (Deserialization of Untrusted Data) |
| EPSS | 0.604 (98.31st percentile) |
| CISA KEV | No |
| Fix available | 6.0.0 (unsafe deserialization methods removed) |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.8 (EPSS ≥ 0.50) |
| **Contextual score** | **9.8 × (0.3×0.4 + 0.8×0.4 + 0.8×0.2) = 9.8 × 0.60 = 5.9** |
| NEN-7510 | 8.8, 8.28 |
| Decision | **ACCEPT** - Fix requires upgrading to Spring 6.0.0, which is incompatible with OpenMRS 1.9.9. Upgrade is blocked by the platform dependency. Flag for risk register in #37. Mitigation: do not expose Spring's RemoteInvocationSerializingExporter endpoints externally. |

---

#### C-03 - CVE-2015-7501 - commons-collections

| Field | Value |
|-------|-------|
| Package | commons-collections:commons-collections 3.2 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-502 (Deserialization of Untrusted Data) |
| EPSS | 0.715 (98.74th percentile) |
| CISA KEV | No (but historically weaponised against WebSphere, WebLogic, JBoss) |
| Fix available | 3.2.2 |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.8 (EPSS ≥ 0.50) |
| **Contextual score** | **9.8 × 0.60 = 5.9** |
| NEN-7510 | 8.8, 8.28, 5.19 |
| Decision | **PATCH** - Fix available in 3.2.2 (low effort upgrade). This is a platform dependency in OpenMRS, so the upgrade must be coordinated with the OpenMRS core team or applied at deployment level. Highest priority patch candidate among Critical findings. |

---

#### C-04 - CVE-2016-1000031 - commons-fileupload

| Field | Value |
|-------|-------|
| Package | commons-fileupload:commons-fileupload 1.2.1 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-502 (Deserialization of Untrusted Data - DiskFileItem) |
| EPSS | 0.564 (98.16th percentile) |
| CISA KEV | No |
| Fix available | 1.3.3 |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.8 (EPSS ≥ 0.50) |
| **Contextual score** | **9.8 × 0.60 = 5.9** |
| NEN-7510 | 8.8, 8.28, 5.19 |
| Decision | **PATCH** - Fix available in 1.3.3. Platform dependency, same coordination caveat as C-03. |

---

#### C-08 - CVE-2013-7285 - xstream

| Field | Value |
|-------|-------|
| Package | com.thoughtworks.xstream:xstream 1.4.3 |
| Scope | provided |
| CVSS v3.0 | 9.8 - CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-78 (OS Command Injection via XML deserialization) |
| EPSS | 0.188 (95.44th percentile) |
| CISA KEV | No |
| Fix available | 1.4.7 |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.5 (EPSS 0.10-0.49) |
| **Contextual score** | **9.8 × (0.3×0.4 + 0.8×0.4 + 0.5×0.2) = 9.8 × 0.54 = 5.3** |
| NEN-7510 | 8.8, 8.28, 5.19 |
| Decision | **PATCH** - Fix in 1.4.7. Note: xstream 1.4.3 has 22+ High/Critical CVEs; a single version upgrade to a current release addresses the majority. Evaluate whether OpenMRS platform can accept xstream ≥ 1.4.20. |

---

#### C-09 - CVE-2019-13990 - quartz

| Field | Value |
|-------|-------|
| Package | org.quartz-scheduler:quartz 2.1.1 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-611 (XML External Entity Injection) |
| EPSS | 0.138 (94.44th percentile) |
| CISA KEV | No |
| Fix available | 2.3.2 |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.5 (EPSS 0.10-0.49) |
| **Contextual score** | **9.8 × 0.54 = 5.3** |
| NEN-7510 | 8.8, 8.28, 5.19 |
| Decision | **PATCH** - Fix in 2.3.2. Quartz is used for scheduling jobs in OpenMRS. Exploit requires attacker-supplied XML job definitions, which requires write access to the Quartz configuration. |

---

#### C-10 - CVE-2019-10202 - jackson-mapper-asl

| Field | Value |
|-------|-------|
| Package | org.codehaus.jackson:jackson-mapper-asl 1.5.0 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-502 (Deserialization of Untrusted Data) |
| EPSS | 0.072 (91.81st percentile) |
| CISA KEV | No |
| Fix available | None - jackson-mapper-asl is EOL (replaced by jackson-databind) |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.3 (EPSS 0.01-0.09) |
| **Contextual score** | **9.8 × (0.3×0.4 + 0.8×0.4 + 0.3×0.2) = 9.8 × 0.50 = 4.9** |
| NEN-7510 | 8.8, 5.19 |
| Decision | **ACCEPT** - EOL library, no fix available. Upgrade requires OpenMRS platform migration to jackson-databind. Flag for risk register in #37. |

---

#### C-11 - CVE-2020-10683 - dom4j

| Field | Value |
|-------|-------|
| Package | dom4j:dom4j 1.6.1 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-611 (XML External Entity Injection) |
| EPSS | 0.070 (91.63rd percentile) |
| CISA KEV | No |
| Fix available | None for 1.x - dom4j 1.x is EOL |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.3 (EPSS 0.01-0.09) |
| **Contextual score** | **9.8 × 0.50 = 4.9** |
| NEN-7510 | 8.8, 5.19 |
| Decision | **ACCEPT** - EOL library. Fix requires migrating to dom4j 2.x, which is blocked by OpenMRS platform dependencies. Flag for risk register in #37. |

---

#### C-12 - CVE-2021-23926 - xmlbeans

| Field | Value |
|-------|-------|
| Package | org.apache.xmlbeans:xmlbeans 2.3.0 |
| Scope | provided |
| CVSS v3.1 | 9.1 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:H |
| CWE | CWE-776 (Improper Restriction of Recursive Entity References in DTDs) |
| EPSS | 0.004 (63.80th percentile) |
| CISA KEV | No |
| Fix available | 3.0.0 |
| bereikbaarheid | 0.3 (provided scope, intranet) |
| healthcare-impact | 0.8 |
| exploit | 0.1 (EPSS < 0.01) |
| **Contextual score** | **9.1 × (0.3×0.4 + 0.8×0.4 + 0.1×0.2) = 9.1 × 0.46 = 4.2** |
| NEN-7510 | 8.8, 5.19 |
| Decision | **PATCH** - Fix in 3.0.0. Low EPSS reduces urgency but fix is available. |

---

#### C-13 - CVE-2026-40076 - openmrs-web

| Field | Value |
|-------|-------|
| Package | org.openmrs.web:openmrs-web 1.9.9 |
| Scope | provided |
| CVSS v4.0 | 9.4 - CVSS:4.0/AV:N/AC:L/AT:N/PR:H/UI:N/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H |
| CWE | CWE-22 (Path Traversal - ZIP Slip via module upload) |
| EPSS | 0.001 (31.08th percentile) |
| CISA KEV | No |
| Fix available | No fix available for OpenMRS 1.9.9 |
| bereikbaarheid | 0.2 (requires authenticated admin access - PR:H in CVSS vector) |
| healthcare-impact | 0.8 |
| exploit | 0.1 (EPSS < 0.01, newly published CVE) |
| **Contextual score** | **9.4 × (0.2×0.4 + 0.8×0.4 + 0.1×0.2) = 9.4 × 0.42 = 3.9** |
| NEN-7510 | 8.8, 8.29, 5.19 |
| Note | Uses CVSSv4.0, not v3.1. The PR:H prerequisite (admin authentication) significantly limits the attack surface in a properly managed deployment. |
| Decision | **ACCEPT** - No fix available. Admin account compromise is a prerequisite; mitigated by strong access controls on admin accounts and audit logging. Flag for risk register in #37. |

---

#### C-14 - CVE-2022-0839 - liquibase-core

| Field | Value |
|-------|-------|
| Package | org.liquibase:liquibase-core 2.0.5 |
| Scope | provided |
| CVSS v3.1 | 9.8 - CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H |
| CWE | CWE-611 (XML External Entity Injection) |
| EPSS | 0.001 (27.78th percentile) |
| CISA KEV | No |
| Fix available | 4.8.0 |
| bereikbaarheid | 0.2 (Liquibase runs at application startup/migration time only, not during normal request handling) |
| healthcare-impact | 0.8 |
| exploit | 0.1 (EPSS < 0.01) |
| **Contextual score** | **9.8 × (0.2×0.4 + 0.8×0.4 + 0.1×0.2) = 9.8 × 0.42 = 4.1** |
| NEN-7510 | 8.8, 5.19 |
| Decision | **PATCH** - Fix in 4.8.0. Liquibase changelog files should be treated as trusted internal resources; exploitation requires attacker write access to changelogs. Low EPSS but fix is available. |

---

## 6. High Severity - Grouped Summary

61 High severity findings were identified. Individual per-finding analysis follows the same methodology as section 5.2. The table below groups findings by package to allow prioritisation.

| Package | Version | High CVE count | Fix available | Recommended action |
|---------|---------|---------------|---------------|--------------------|
| xstream | 1.4.3 | ~18 | Yes (1.4.20+) | PATCH - single upgrade addresses most findings |
| spring-* | 3.0.5.RELEASE | ~12 | 5.3.x / 6.0.0 | ACCEPT - blocked by OpenMRS 1.9.9 compatibility |
| hibernate-core | 3.6.10.Final | ~6 | Yes | PATCH - evaluate with OpenMRS compatibility |
| mysql-connector-java | 5.1.28 | ~5 | Yes (8.x) | PATCH - connector upgrade is independent of platform |
| commons-beanutils | (via spring) | ~4 | Yes | PATCH |
| log4j | 1.2.15 | ~4 | None (EOL) | ACCEPT - EOL, no server-side path for most |
| Others | various | ~12 | Mixed | Individual analysis required |

Full individual analysis of High findings to be completed during sprint 3 prioritisation.

---

## 7. Medium and Low Severity - Summary

| Severity | Count | General finding pattern | Recommended action |
|----------|-------|------------------------|-------------------|
| Medium | 54 | Primarily information disclosure, denial of service, and SSRF vulnerabilities in older library versions | Review individually; most are `provided` scope |
| Low | 5 | Minor information disclosure vulnerabilities | Accept pending individual review |

---

## 8. NEN-7510:2024-2 Control Mapping

| Control | Description | Findings addressed |
|---------|-------------|-------------------|
| 8.8 | Management of technical vulnerabilities | All Critical findings - this analysis is the primary evidence artifact |
| 8.28 | Secure coding | C-02, C-03, C-04, C-08 (deserialization and injection vulnerabilities in directly used libraries) |
| 8.29 | Security testing in development and acceptance | C-13 (module upload path traversal - testable through security testing) |
| 5.19 | Information security in supplier relationships | All `provided` findings - dependency on OpenMRS platform for patching supplied components |
| 5.22 | Monitoring, review and change management of supplier services | All EOL packages (log4j 1.x, dom4j 1.x, jackson-mapper-asl) - no upstream vendor support |

---

## 9. Summary for Downstream Issues

### For #31 - Risk Matrix

| Tier | Count | Contextual score range | Findings |
|------|-------|----------------------|---------|
| Active Critical | 10 | 3.9 - 5.9 | C-02 through C-14 (excl. suppressed) |
| Suppressed | 4 | N/A | FP-01 (Spring4Shell JDK 8), FP-02 (Chainsaw), FP-03 (JDBCAppender), FP-04 (SocketServer) |
| High | 61 | TBD | Pending individual analysis |
| Medium | 54 | TBD | Pending individual analysis |
| Low | 5 | TBD | Pending individual analysis |

Highest contextual scores: **C-02, C-03, C-04** (score 5.9 each) - these should appear in the top risk tier.

### For #35 - Security Backlog

**PATCH candidates (fix available):**
- C-03: commons-collections → 3.2.2
- C-04: commons-fileupload → 1.3.3
- C-08: xstream → 1.4.7 (or current release)
- C-09: quartz → 2.3.2
- C-12: xmlbeans → 3.0.0
- C-14: liquibase-core → 4.8.0

**ACCEPT candidates (flag for RAR):**
- C-02: spring-web (no compatible fix)
- C-10: jackson-mapper-asl (EOL)
- C-11: dom4j (EOL 1.x)
- C-13: openmrs-web (no fix, requires admin auth)

### For #37 - Risk Assessment Report

The following accepted findings must appear in the risk register:

| Finding | Risk description | Compensating control |
|---------|-----------------|---------------------|
| C-02 | Spring unsafe deserialization in provided platform | Network isolation; do not expose Spring RMI endpoints |
| C-10 | jackson-mapper-asl EOL deserialization | Restrict untrusted JSON deserialization at application level |
| C-11 | dom4j XXE in provided platform | Disable external entities at deployment XML parser configuration |
| C-13 | openmrs-web path traversal via module upload | Restrict admin access; audit all module uploads; monitor file system writes |

---

## 10. Evidence Files

| File | Description |
|------|-------------|
| `openmrs-module-appointmentscheduling/bom.json` | CycloneDX 1.4 SBOM - source for scope classification |
| `sca-report/grype-report.json` | Full Grype scan output - raw findings input |
| `documentation/sbom.md` | SBOM generation and toolchain documentation |
| `documentation/sbom-analysis.md` | This document - verified, scored findings list |

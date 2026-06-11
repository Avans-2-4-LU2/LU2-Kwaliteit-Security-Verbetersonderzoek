# Software Bill of Materials (SBOM) & Software Composition Analysis (SCA)

## Overview

This document describes how the SBOM for the OpenMRS Appointment Scheduling Module is generated, how the SCA scan is executed, and what the findings mean.

**NEN-7510:2024-2 controls addressed:** 8.8 (Management of technical vulnerabilities), 8.29 (Security testing in development and acceptance), 5.23 (Information security for use of cloud services)

---

## Toolchain

| Step | Tool | Version | Output |
|------|------|---------|--------|
| SBOM generation | CycloneDX Maven Plugin | 2.7.9 | `bom.json` (CycloneDX 1.4, JSON) |
| SCA scan | Grype (Anchore) | 0.114.0 | `sca-report/grype-report.json` |

**Why CycloneDX?** It is the industry standard for machine-readable SBOMs, natively supported by `anchore/sbom-action` (used in the CI pipeline) and required for NEN-7510 traceability.

**Why Grype?** It is the SCA engine behind `anchore/sbom-action` and `anchore/scan-action` in GitHub Actions. It requires no API key, uses its own vulnerability database (built from GitHub Advisory Database, NVD, OSV, and more), and can scan a CycloneDX SBOM directly.

---

## Generating the SBOM

```bash
mvn org.cyclonedx:cyclonedx-maven-plugin:2.7.9:makeAggregateBom
```

Output: `openmrs-module-appointmentscheduling/bom.json`

The aggregate goal traverses all Maven modules (`api`, `omod`) and consolidates their dependency trees into one SBOM. Test-scoped dependencies are excluded (`includeTestScope=false`).

**Component count:** 111 components

Notable components included:

| Component | Version | Scope |
|-----------|---------|-------|
| spring-core / spring-webmvc | 3.0.5.RELEASE | provided |
| log4j | 1.2.15 | provided |
| hibernate-core | 3.6.10.Final | provided |
| commons-collections | 3.2 | provided |
| xstream | 1.4.3 | provided |
| mysql-connector-java | 5.1.28 | provided |
| joda-time | 2.2 | compile |

---

## Running the SCA Scan

```bash
.\grype.exe sbom:./bom.json -o json --file sca-report/grype-report.json
```

To view results in the terminal (table format):

```bash
.\grype.exe sbom:./bom.json -o table
```

---

## SCA Findings Summary

Scan date: 2026-06-08  
Input: `bom.json` (111 components)

| Severity | Count |
|----------|-------|
| Critical | 15 |
| High | 61 |
| Medium | 54 |
| Low | 5 |
| **Total** | **135** |

---

## License Findings

59 of 111 components include license metadata. The remaining 52 (primarily OpenMRS platform dependencies) do not declare a license in their POM; their license (MPL-2.0) is documented at the OpenMRS project level.

| License | Count | Risk |
|---------|-------|------|
| Apache-2.0 | 32 | Low — permissive |
| MIT | 3 | Low — permissive |
| BSD-3-Clause / BSD-4-Clause | 3 | Low — permissive |
| CDDL-1.0 / MPL-1.1 | 3 | Medium — weak copyleft, file-level |
| LGPL-2.1-only | 1 | Low in context — module is not distributed standalone |
| Unknown | 52 | Tracked at OpenMRS platform level |

No GPL or AGPL licenses were identified. No license conflicts with the project's distribution model.

---

## Critical Vulnerabilities

| CVE / GHSA | Package | Version | Fix Available |
|------------|---------|---------|---------------|
| GHSA-36p3-wjmg-h94x | spring-beans, spring-webmvc | 3.0.5.RELEASE | 5.2.20.RELEASE |
| GHSA-4wrc-f8pq-fpqp | spring-web | 3.0.5.RELEASE | 6.0.0 |
| GHSA-fjq5-5j5f-mvxh | commons-collections | 3.2 | 3.2.2 |
| GHSA-7x9j-7223-rg5m | commons-fileupload | 1.2.1 | 1.3.3 |
| GHSA-2qrg-x229-3v8q | log4j | 1.2.15 | No fix (EOL) |
| GHSA-65fg-84f6-3jq3 | log4j | 1.2.15 | No fix (EOL) |
| GHSA-f7vh-qwp3-x37m | log4j | 1.2.15 | No fix (EOL) |
| GHSA-f554-x222-wgf7 | xstream | 1.4.3 | 1.4.7 |
| GHSA-9qcf-c26r-x5rf | quartz | 2.1.1 | 2.3.2 |
| GHSA-c27h-mcmw-48hv | jackson-mapper-asl | 1.5.0 | No fix (EOL) |
| GHSA-hwj3-m3p6-hj38 | dom4j | 1.6.1 | No fix (EOL) |
| GHSA-mw3r-pfmg-xp92 | xmlbeans | 2.3.0 | 3.0.0 |
| GHSA-78fc-9688-w8xw | openmrs-web | 1.9.9 | No fix |
| GHSA-jvfv-hrrc-6q72 | liquibase-core | 2.0.5 | 4.8.0 |

---

## Risk Assessment

The high number of findings is expected given the age of the project. This module targets **OpenMRS 1.9.9** (released ~2013), which pins most dependencies at 2010–2013 vintage versions. Many of these dependencies are transitively pulled in through the OpenMRS platform and are declared as `provided` scope, meaning they are not bundled in this module's artifact but come from the host application.

**Key risk areas:**

1. **Log4j 1.2.15 (EOL)** — Log4j 1.x reached end-of-life in 2015. Three critical advisories with no available fix. Mitigation requires migrating to Log4j 2.x at the OpenMRS platform level.

2. **commons-collections 3.2** — Affected by the well-known Java deserialization RCE vulnerability. Fixed in 3.2.2. This is a `provided` dependency from OpenMRS core.

3. **xstream 1.4.3** — 22 High/Critical CVEs related to XML deserialization attacks. Fixed in newer versions. Also a platform dependency.

4. **Spring Framework 3.0.5.RELEASE** — Severely outdated (current: 6.x). Multiple Critical and High CVEs. Upgrade is blocked by OpenMRS 1.9.9 compatibility.

5. **jackson-mapper-asl / dom4j / openmrs-web** — EOL libraries with no upstream fix available.

**Scope note:** Because the vulnerable packages are `provided` by the OpenMRS platform (not bundled by this module), exploitation depends on the deployment environment's OpenMRS version and configuration. In a controlled hospital intranet deployment (as typical for OpenMRS), exposure to remote attackers is significantly reduced.

---

## Recommendations

| Priority | Action |
|----------|--------|
| High | Upgrade `commons-collections` to 3.2.2 (has a fix, low effort) |
| High | Evaluate migration to OpenMRS 2.x, which uses updated platform dependencies |
| Medium | Track CVEs in `provided` dependencies via Dependabot alerts on GitHub |
| Medium | Add `anchore/scan-action` to the CI pipeline to block PRs introducing new Critical CVEs |
| Low | Accept residual risk for EOL platform libraries with no fix; document in risk register |

---

## Files

| File | Description |
|------|-------------|
| `openmrs-module-appointmentscheduling/bom.json` | CycloneDX 1.4 SBOM (111 components) |
| `sca-report/grype-report.json` | Full Grype scan output (JSON) |

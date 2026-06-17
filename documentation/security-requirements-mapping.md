# Security Requirements Mapping

## Purpose

This document captures the security risk treatment plan for the OpenMRS appointment scheduling module. It translates the risk assessment and CIA analysis into actionable security requirements, prioritizes them, maps them to NEN-7510 Annex A controls, and defines verification criteria.

## Scope

- All unacceptable and high-risk findings from `documentation/cia-analysis.md`
- Security treatment actions from `documentation/sbom-analysis.md`
- Existing security requirement framework in `documentation/Requirements.md`

## Approach

1. Identify unacceptable risks from the CIA analysis risk register.
2. Translate each treatment action into an actionable requirement or backlog story.
3. Map each requirement to NEN-7510 controls and Annex A control families.
4. Prioritize using risk severity, implementation dependencies, and effort.
5. Document verification criteria and evidence artifacts.
6. Review the prioritized backlog with the Product Owner and development team.

## Risk Treatment Backlog

| ID | Requirement | Risk Addressed | NEN-7510 Control | Priority | Verification Criteria |

|----|-------------|----------------|------------------|----------|----------------------|
| SR-01 | Enforce role-based access control for all appointment records and scheduling data. Restrict read/write access to authorized roles only, including confidential appointment types. | Appointment records exposure; appointment type disclosure | 8.2, 8.26, 8.28 | Must do | Access control tests show unauthorized users are denied; code review confirms privilege checks on all entry points; unit/integration tests for confidential appointment type flag. |
| SR-02 | Protect appointment requests, free-text notes, and cancellation reasons with least-privilege access and data classification rules. | Exposure of clinical context in appointment requests/notes | 8.2, 8.15, 8.26 | Must do | Test cases verify restricted fields are hidden or masked for unauthorized users; review confirms no sensitive fields are logged; sensitive data fields documented in data inventory. |
| SR-03 | Ensure the `confidential` appointment type privilege is enforced consistently in UI and API workflows. | Confidential appointment type misclassification / unauthorized access | 8.2, 8.26, 8.28 | Must do | End-to-end tests prove confidential appointment types are not visible/accessible by users without privilege; PR review confirms UI and API enforcement. |
| SR-04 | Implement integrity controls for appointment blocks, time slots, and provider schedules, including validation and concurrency handling for create/update operations. | Schedule corruption, double-booking, missed appointments | 8.26, 8.29 | Must do | Unit tests validate schedule consistency and reject invalid state transitions; code inspection confirms transactional updates and conflict handling. |
| SR-05 | Harden audit trail data: ensure audit metadata and status history cannot be modified without proper authorization and are logged immutably. | Tampering with appointment history and audit records | 8.15, 8.26, 9 | Should do | Audit trail tests verify changes are recorded and history fields are immutable; documentation describes retention and review controls. |
| SR-06 | Add targeted secure coding checks for free-text fields and XML/serialization dependencies to prevent injection and deserialization risks. | Unstructured input abuse and library-based deserialization flaws | 8.28, 8.29, 8.8 | Should do | Static analysis and dependency scan results show resolved high-risk findings; tests cover input validation and safe parsing behaviors. |
| SR-07 | Create a prioritized dependency remediation backlog for critical library vulnerabilities and EOL components, with patch/accept decisions explicitly recorded. | Technical vulnerability exploitation through dependencies | 8.8, 5.19, 8.29 | Must do | Backlog contains remediation tickets for patchable findings and documented acceptance decisions for unavoidable residual risks; SBOM and scan artefacts referenced. |
| SR-08 | Review and strengthen CI/CD security gates, ensuring failing security checks block merges and deploys for high-risk issues. | Unsafe code or dependency changes reaching main branches | 8.25, 8.29, 8.31 | Should do | Pipeline configuration reviewed; branch protection requires passing security checks; evidence in workflow definitions and PR status checks. |

## Prioritization Criteria

- **Must do**: direct mitigation of unacceptable risks (risk score 15-25) and controls required before release.
- **Should do**: high risk or strong mitigation value with moderate effort and dependencies on core changes.
- **Could do**: valuable improvements that can follow after the must/should items.
- Prioritization also accounts for dependencies: access control and confidentiality enforcement come before audit/logging enhancements.

## Traceability Matrix

| Risk / Finding | Treatment Requirement | NEN-7510 Control | Evidence Artifact|

| ----------------------- | --------------------- | ---------------- | ------------------------- |
| Appointment records unauthorized access | SR-01 | 8.2, 8.26, 8.28 | `documentation/cia-analysis.md`, access control tests, code review notes |
| Confidential appointment type disclosure | SR-03 | 8.2, 8.26, 8.28 | `documentation/cia-analysis.md`, E2E tests, UI/API review |
| Appointment requests and notes exposure | SR-02 | 8.2, 8.15, 8.26 | `documentation/cia-analysis.md`, data inventory, test cases |
| Schedule integrity corruption | SR-04 | 8.26, 8.29 | Validation tests, transaction review, data model analysis |
| Audit history tampering | SR-05 | 8.15, 8.26, 9 | Audit trail tests, `documentation/review-documentation.md` |
| Deserialization / dependency vulnerabilities | SR-06, SR-07 | 8.8, 8.28, 8.29 | `documentation/sbom-analysis.md`, dependency scan report, backlog tickets |
| CI/CD security gaps | SR-08 | 8.25, 8.29, 8.31 | `documentation/cicd-risk-evaluation.md`, pipeline config, branch protection documentation |

## Review and Alignment

- Present this backlog to the Product Owner and development team.
- Confirm each requirement is accepted, prioritized, and assigned to a sprint.
- Record review decisions in `documentation/cia-review-meeting-notes.md` or equivalent meeting notes.
- Update the backlog/board with the final ticket IDs and links to this mapping document.

## Next Actions

1. Add the above requirements to the central backlog or project board as stories/tasks.
2. Link each story/task to this document and to the relevant NEN-7510 control.
3. Review with the Product Owner and team and capture approval in the meeting notes.
4. Execute the highest-priority mitigation tasks in sprint planning.

## Notes

- This plan is aligned with the existing requirement IDs S-16, S-17, and S-18 in `documentation/Requirements.md`.
- The evidence artifacts should include the risk register, SBOM results, scan outputs, and PR/test artifacts.

## Security Requirements (Backlog Items)

### SR-01: Enforce Role-Based Access Control for Appointment Data

**Description**  
The appointment scheduling module processes sensitive patient data (appointment records, confidential appointment types, provider schedules). Unauthorized access to these records poses critical confidentiality and integrity risks. Unacceptable risk: appointment records (score 20), appointment types (score 15).

**Requirement**  
NFR - Security / Access Control (NEN-7510 8.2, 8.26)

**NEN 7510 Relevance**  
NEN-7510-1 (6.1.3) requires implementing controls from Annex A to treat identified risks. NEN-7510-2 controls 8.2 (access control) and 8.26 (application security requirements) mandate role-based authorization, least-privilege enforcement, and documented access policies. Role-based access control is a foundational control for all data protection.

**Tasks**

- [ ] Review the existing privilege model in `AppointmentSchedulingConstants.java` and `AppointmentService.java`
- [ ] Identify all public-facing entry points (UI, API, service methods) that access appointment records
- [ ] Document the privilege requirements for each entry point (read, write, delete for each appointment type)
- [ ] Implement or enhance role checks on all entry points using existing or new privileges
- [ ] Create unit and integration tests for authorized and denied access scenarios
- [ ] Add test cases specifically for the `confidential` appointment type privilege
- [ ] Perform code review to verify privilege checks are in place and follow the principle of least privilege
- [ ] Document the access control design in `documentation/access-control-design.md`

**Acceptance Criteria**

- All unacceptable-risk appointment data entry points enforce role-based authorization
- Unauthorized users receive access denied errors; no sensitive data is leaked in error messages
- Unit/integration tests achieve ≥95% coverage of access control logic
- Confidential appointment types are consistently blocked for users without the required privilege (in UI and API)
- Code review approves the privilege checks and documents any exceptions
- No regression in appointment functionality for authorized users
- Access control design document is approved by the security team and Product Owner

---

### SR-02: Protect Sensitive Fields in Appointment Requests and Free-Text Notes

**Description**  
Appointment request notes and free-text fields (reason, cancel_reason) can disclose clinical context or private circumstances. These fields pose confidentiality risks (score 15) if exposed to unauthorized users or logged in plaintext.

**Requirement**  
NFR - Security / Data Classification & Masking (NEN-7510 8.2, 8.15, 8.26)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.15 (logging and monitoring) requires ensuring sensitive data is not logged. Control 8.2 (access control) and 8.26 (application security) require identifying and protecting special-category personal data. Data classification and field-level protection are evidence of deliberate data protection.

**Tasks**

- [ ] Document all sensitive fields in the data inventory: reason, cancel_reason, appointment request notes, phone number fields
- [ ] Review current logging statements in `AppointmentService.java`, `AppointmentRequest.java`, and related classes for sensitive field exposure
- [ ] Implement data classification annotations or markers on sensitive fields
- [ ] Remove or mask sensitive fields from logging statements
- [ ] Implement field-level access control for sensitive fields: hide/mask them for users without appropriate privilege
- [ ] Add tests to verify sensitive fields are masked for unauthorized users
- [ ] Add tests to verify sensitive fields are NOT logged in application logs
- [ ] Create test data that includes sensitive content and verify it is never exposed

**Acceptance Criteria**

- Sensitive fields are formally documented in the data inventory with classification (e.g., personal, health-related, confidential)
- No sensitive field values appear in application logs or debug output
- Unauthorized users do not see sensitive field content in UI or API responses
- Test suite includes cases for all sensitive fields with verification that masking/hiding works
- Code review confirms no plaintext logging of classified fields
- Documentation updated with data protection policy

---

### SR-03: Enforce Confidential Appointment Type Privilege Consistently

**Description**  
The module includes a `confidential` appointment type flag intended to restrict access. Inconsistent enforcement (missing checks in one UI path or API endpoint) creates unauthorized access risk (score 15). The privilege exists in code but its enforcement is not fully verified across all entry points.

**Requirement**  
NFR - Security / Confidential Data Authorization (NEN-7510 8.2, 8.26, 8.28)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.28 (secure coding) requires developers to implement authorization correctly at every entry point. Control 8.26 (application security requirements) mandates consistent enforcement of privilege checks across the application. Control 8.2 (access control) requires mechanisms to enforce least-privilege access to sensitive resources.

**Tasks**

- [ ] Map all code paths that retrieve, display, or modify appointment types (UI, API, batch processes, reports)
- [ ] Verify the confidential flag check is present in every entry point
- [ ] Add missing privilege checks where the confidential flag is not evaluated
- [ ] Create end-to-end tests for each entry point: verify confidential appointments are hidden for unauthorized users and visible for authorized users
- [ ] Test cross-component scenarios: appointment type created, assigned to an appointment, displayed in UI, exported in report
- [ ] Perform code inspection to ensure the privilege check is applied BEFORE rendering confidential appointment content

**Acceptance Criteria**

- Privilege check for confidential appointments is present in 100% of entry points (UI, REST API, scheduled jobs, reports)
- End-to-end tests verify confidential appointments are not accessible, visible, or exposed by users without the privilege
- Code review confirms privilege checks are applied at the earliest point in the request flow
- No confidential appointment data leaks through error messages, logs, or data exports
- Unauthorized users attempting to access confidential appointments receive a consistent, secure error response

---

### SR-04: Implement Integrity Controls for Appointment Blocks and Time Slots

**Description**  
Appointment blocks, time slots, and provider schedules must be accurate to prevent double-booking, missed appointments, and operational disruption. Unacceptable risk: score 15 (integrity of patient care). Controls must include validation, transactional integrity, and conflict detection.

**Requirement**  
NFR - Security / Data Integrity & Validation (NEN-7510 8.26, 8.29)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.26 (application security requirements) includes business logic validation and data integrity verification. Control 8.29 (security testing) requires testing to ensure invalid state transitions are rejected. Data integrity is essential for appointment scheduling correctness and supports patient safety.

**Tasks**

- [ ] Review the data model for `AppointmentBlock`, `TimeSlot`, and `ProviderSchedule` to identify integrity constraints
- [ ] Identify all places where appointment blocks/time slots are created or modified
- [ ] Implement validation rules: no overlapping time slots, no past dates, provider must be available, no conflicts with existing blocks
- [ ] Use database constraints (unique indexes, foreign keys) and application-level validation (business rule checks)
- [ ] Test transactional integrity: ensure concurrent updates do not corrupt state or create race conditions
- [ ] Create unit tests for all validation rules with valid and invalid data
- [ ] Create integration tests for concurrent appointment creation/modification
- [ ] Document the schedule integrity requirements and validation rules

**Acceptance Criteria**

- All validation rules are enforced at both database and application levels
- Unit tests achieve ≥90% coverage of validation logic and cover both valid and invalid scenarios
- Concurrent modification tests pass without data corruption or race conditions
- Overlapping or conflicting time slots are rejected with clear error messages
- No past dates or invalid schedule data can be created
- Code review confirms transactional boundaries are correctly applied
- Documentation specifies the integrity constraints and their business rationale

---

### SR-05: Harden Audit Trail Data and Prevent Tampering

**Description**  
The module includes audit metadata (creator, changed-by, void reasons, status history) to support accountability and forensic review. These records must be tamper-resistant and access-restricted to maintain their integrity for incident investigation and compliance audits.

**Requirement**  
NFR - Security / Audit Immutability & Access Control (NEN-7510 8.15, 8.26, Chapter 9)

**NEN 7510 Relevance**  
NEN-7510-1 Chapter 9 (internal audit and compliance) requires maintaining audit records and evidence of control implementation. NEN-7510-2 control 8.15 (logging) requires that logged events are protected and not modified after recording. Control 8.26 (application security requirements) mandates that audit data is protected from unauthorized modification.

**Tasks**

- [ ] Review the audit fields in the data model: creator, changed-by, voided-by, timestamps, void reasons, appointment status history
- [ ] Identify who can view and who can modify audit fields (authorization model)
- [ ] Implement or verify that audit fields are immutable (read-only after creation) in the application
- [ ] Remove any code that allows deletion or modification of historical records (except through explicit, audited admin actions)
- [ ] Create tests to verify audit fields cannot be modified by normal application operations
- [ ] Document the audit retention policy and access restrictions
- [ ] Design and implement alerting for suspicious audit modifications (admin-only operations)

**Acceptance Criteria**

- Audit fields (creator, timestamps) are immutable for normal user operations
- Audit data is protected from unauthorized viewing (access-controlled like sensitive appointment data)
- Attempts to modify audit records are logged and trigger alerts (admin actions)
- Test cases verify audit records survive application updates and are never lost
- Documentation specifies the audit retention period and justifies it (e.g., minimum 7 years per healthcare regulations)
- Compliance review approves the immutability controls

---

### SR-06: Address Deserialization and Injection Risks in Secure Coding

**Description**  
Dependency scan results show high-risk deserialization (e.g., Spring unsafe serialization, XStream) and injection vulnerabilities in third-party libraries. Secure coding practices must be applied to all XML, JSON, and user input processing to prevent exploitation.

**Requirement**  
NFR - Security / Secure Input Handling (NEN-7510 8.28, 8.29, 8.8)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.28 (secure coding) mandates secure handling of untrusted input and safe use of dangerous libraries. Control 8.29 (security testing) requires testing for injection and deserialization flaws. Control 8.8 (technical vulnerabilities) requires assessment and remediation of library vulnerabilities with known exploits.

**Tasks**

- [ ] Review the appointment module for use of serialization, XML parsing, JSON processing, and dynamic code evaluation
- [ ] Identify all user inputs (UI forms, API payloads, file uploads) that are parsed or deserialized
- [ ] Apply secure coding guidelines: whitelist validation, safe parsing (disable external entities), use safe libraries (e.g., Jackson over jackson-mapper-asl)
- [ ] Create unit tests for injection attacks: SQL injection, XML injection (XXE), JSON injection, command injection
- [ ] Run SAST (SonarCloud, CodeQL) on all changes and verify no new injection vulnerabilities are introduced
- [ ] Document the secure input handling policy and the rationale for library choices

**Acceptance Criteria**

- All user inputs are validated against a whitelist before processing
- No deserialization of untrusted data without safe guards (use ObjectInputStream filters or equivalent)
- XML parsing disables external entities (XXE protection)
- SAST scans show zero new injection vulnerabilities
- Unit tests cover common injection attack patterns (SQL, XXE, XPath, command injection)
- Code review confirms secure coding practices are applied

---

### SR-07: Create and Prioritize Dependency Remediation Backlog

**Description**  
SBOM and dependency scans identified critical vulnerabilities in third-party libraries (e.g., commons-collections, xstream, spring-web). Each finding requires an explicit decision: patch, upgrade, accept with compensating control, or mitigate. The backlog ensures tracking and accountability.

**Requirement**  
NFR - Security / Technical Vulnerability Management (NEN-7510 8.8, 5.19, 8.29)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.8 (management of technical vulnerabilities) requires identifying, assessing, and remediating vulnerabilities. Control 5.19 (information security in supplier relationships) requires managing the security of supplied components and dependencies. A prioritized, traceable backlog is the operational evidence that vulnerabilities are being managed.

**Tasks**

- [ ] Review all findings in `documentation/sbom-analysis.md` and `sca-report/grype-report.json`
- [ ] For each critical/high-risk finding, create a decision record: PATCH (upgrade available), ACCEPT (no fix available, compensating control documented), or MITIGATE (workaround implemented)
- [ ] Prioritize patch candidates by exploitability (CVSS, EPSS), likelihood of attack, and remediation effort
- [ ] Document accepted risks with compensating controls (e.g., network isolation for Spring RMI, disabling external XML entities for dom4j XXE)
- [ ] Create backlog tickets for all patchable vulnerabilities with links to the scan results and decision record
- [ ] Link each backlog ticket to the relevant NEN-7510 control
- [ ] Track patch/accept decisions in a central traceability document

**Acceptance Criteria**

- All critical and high-risk findings from the SBOM scan have an explicit decision (PATCH, ACCEPT, or MITIGATE)
- Patchable vulnerabilities are in the backlog with priority (MoSCoW or Risk-Value matrix) and assigned to a sprint
- Accepted risks are documented with compensating controls and approval from the security team/Product Owner
- Traceability matrix links each vulnerability to its decision, control, and evidence artifacts
- No unaddressed high-risk vulnerabilities remain without documented justification
- Backlog is reviewed and prioritized with the team

---

### SR-08: Strengthen CI/CD Security Gates and Branch Protection

**Description**  
The CI/CD pipeline must enforce security checks that block merges or deployments when vulnerabilities, policy violations, or security test failures are detected. Current gaps: security checks may not be blocking, or high-risk findings may bypass gates.

**Requirement**  
NFR - Security / Build Pipeline & Deployment Control (NEN-7510 8.25, 8.29, 8.31)

**NEN 7510 Relevance**  
NEN-7510-2 control 8.25 (secure software development lifecycle) requires security activities integrated into the development process. Control 8.29 (security testing) requires test results to be enforced in the pipeline. Control 8.31 (separation of environments) includes controlling code promotion to production with approval gates. Blocking merges on security failures is the operational realization of these controls.

**Tasks**

- [ ] Review current CI/CD pipeline configuration in `.github/workflows/` and branch protection rules
- [ ] Verify that SAST (SonarCloud, CodeQL), SCA (dependency scanning), and SBOM generation steps are present
- [ ] Confirm that failing security checks (e.g., new vulnerabilities, quality gate breach, SAST findings) block PR merge
- [ ] Add or enhance branch protection rules: require status checks to pass (security gates), require approvals before merge to main/dev
- [ ] Configure security gates to fail on: Critical/Blocker SAST findings, High-risk dependencies, SBOM metadata missing
- [ ] Test the gates: create a PR with a known vulnerability and verify it is blocked
- [ ] Document the security gates and exception process (how to override, when, with approval)
- [ ] Add visibility: publish security gate results in PR comments or dashboard

**Acceptance Criteria**

- All security checks (SAST, SCA, dependency scanning) are present and run on every PR
- Failing security checks prevent merge to main and dev branches
- Branch protection requires peer review and security gate approval before production deployment
- Exceptions to security gates require documented approval and are tracked in an exceptions log
- Pipeline configuration is version-controlled and documented
- Test case demonstrates security gate blocking a PR with a high-risk vulnerability
- Team is trained on the security gates and exception process

---

## Prioritization Criteria

- **Must do**: SR-01, SR-02, SR-03, SR-04, SR-07 — direct mitigation of unacceptable risks (15-25) and controls required before release.
- **Should do**: SR-05, SR-06, SR-08 — high risk or strong mitigation value with moderate effort and dependencies on core changes.
- **Could do**: enhancements and hardening that can follow after the must/should items.
- Prioritization also accounts for dependencies: access control and confidentiality enforcement (SR-01, SR-02, SR-03) must be completed before deployment; vulnerability remediation (SR-07) can proceed in parallel; audit hardening (SR-05) is secondary.

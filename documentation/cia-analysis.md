# CIA Analysis - openmrs-module-appointmentscheduling

## 1. Purpose and Scope

This document classifies the key assets of the `openmrs-module-appointmentscheduling` module by Confidentiality, Integrity, and Availability (CIA) and defines the risk criteria used for the risk assessment report.

The scope covers the module's appointment scheduling, appointment request, appointment type, appointment block, time slot, provider schedule, and audit/status history data as implemented in the OpenMRS module source and database mapping files.

## 2. Standards and Legal Basis

- NEN 7510-1 clause 6.1.2 requires a documented information security risk 
  analysis as the basis of the ISMS.
- AVG (GDPR) Article 9 applies because appointment and scheduling data can 
  reveal health-related information, which qualifies as special-category 
  personal data requiring the highest level of protection.

## 3. Sensitive Data Inventory

The module processes the following sensitive or security-relevant data:

- Patient identity and appointment linkage: patient IDs, visit IDs, appointment IDs, and status history in [Appointment.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/Appointment.java) and [liquibase.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/liquibase.xml).
- Appointment notes and reasons: free-text `reason` and `cancel_reason` fields can disclose clinical context or private circumstances in [Appointment.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/Appointment.java) and [Appointment.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/Appointment.hbm.xml).
- Appointment requests: patient, provider, requested-by, time-frame, and notes data in [liquibase.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/liquibase.xml).
- Confidential appointment types: the `confidential` flag in [AppointmentType.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentType.java) and the corresponding database column in [liquibase.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/liquibase.xml).
- Provider schedules and availability: location, provider, dates, times, and schedule mappings in [ProviderSchedule.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/ProviderSchedule.hbm.xml), [AppointmentBlock.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/AppointmentBlock.hbm.xml), and [liquibase.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/liquibase.xml).
- Audit and traceability data: creator, changed-by, voided-by, timestamps, and void reasons in the database model, which are security-relevant because they support accountability and forensic review.
- Patient contact information when used by the UI: the module references a person attribute type for phone number in [AppointmentUtils.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentUtils.java) and shows phone number fields in [messages.properties](../openmrs-module-appointmentscheduling/api/src/main/resources/messages.properties).

## 4. Crown Jewels

The crown jewels are the assets whose compromise would most directly impact patient
privacy, patient safety, or clinical operations.

| Asset | Type | CIA Scores | Threat Scenario |
| --- | --- | --- | --- |
| Appointment records | Data (confidentiality + integrity) | C=5, I=5, A=4 | Data breach, unauthorized access to patient-linked records |
| Appointment requests and notes | Data (confidentiality) | C=5, I=4, A=3 | Disclosure of treatment intent or care pathways |
| Confidential appointment types | Data (confidentiality + integrity) | C=5, I=4, A=2 | Misclassification exposes restricted appointment categories |
| Appointment blocks and time slots | System (availability + integrity) | C=3, I=5, A=5 | Double-booking or missed appointments due to incorrect schedules |
| Audit history and void/retire metadata | Data (integrity) | C=3, I=4, A=3 | Removal or alteration of evidence after a security incident |

## 5. CIA Classification

Scoring scale for each CIA dimension:

- 1 = very low impact
- 2 = low impact
- 3 = moderate impact
- 4 = high impact
- 5 = critical impact

Interpretation:

- Confidentiality: unauthorized disclosure of personal or health-related information.
- Integrity: incorrect, altered, or missing scheduling data that can affect care delivery.
- Availability: inability to view, create, or update appointments when needed for patient care or clinic operations.

| Asset | Confidentiality | Integrity | Availability | Rationale |
| --- | ---: | ---: | ---: | --- |
| Appointment records | 5 | 5 | 4 | Directly identifies a patient and can affect care decisions, visit timing, and follow-up. |
| Appointment requests and notes | 5 | 4 | 3 | Free text can contain sensitive clinical context; integrity matters for triage and booking decisions. |
| Confidential appointment types | 5 | 4 | 2 | Misclassification can expose restricted appointment categories; integrity is important for visibility control. |
| Appointment blocks and time slots | 3 | 5 | 5 | Exposes clinic planning and must remain correct to avoid double-booking or missed appointments. |
| Provider schedules | 3 | 4 | 5 | Mainly operational, but wrong or unavailable schedules can delay care and create booking errors. |
| Appointment status history | 4 | 4 | 3 | Supports accountability and patient journey reconstruction; must remain consistent and available for review. |
| Audit metadata (creator, changed-by, void reasons) | 3 | 4 | 3 | Not usually sensitive alone, but critical for traceability and incident investigation. |
| UI and privilege configuration | 3 | 4 | 4 | Privileges control who can view and edit sensitive scheduling data. |

## 6. Risk Criteria

Risk is scored as:

`Risk score = Impact score x Likelihood score`

Both impact and likelihood use a 1-5 scale, producing a score from 1 to 25.

### Likelihood scale

- 1 = rare
- 2 = unlikely
- 3 = possible
- 4 = likely
- 5 = very likely

### Risk appetite

The project risk appetite is low because the module processes health-related scheduling data.

- Score 1-5: acceptable.
- Score 6-9: acceptable only with documented mitigation and owner approval.
- Score 10-14: high risk; mitigation required before release or deployment.
- Score 15-25: unacceptable; immediate action required.

### Category thresholds

The following thresholds apply by category because patient safety has a higher priority than reputation or routine operational inconvenience.

| Category | Acceptable | Mitigate and review | Unacceptable |
| --- | ---: | ---: | ---: |
| Confidentiality of patient or health data | 1-5 | 6-9 | 10-25 |
| Integrity affecting patient care or schedule correctness | 1-4 | 5-8 | 9-25 |
| Availability for clinical booking operations | 1-5 | 6-9 | 10-25 |
| Reputation-only impact | 1-9 | 10-14 | 15-25 |

If a risk affects both patient safety and privacy, the stricter threshold applies.


## 7. Risk Scoring and Treatment

The CIA classification establishes the baseline impact for each asset. The next step is to combine each asset's CIA impact with a likelihood estimate to calculate a risk score, then select an appropriate treatment strategy (avoid, mitigate, transfer, or accept).

### Risk treatment strategies

- Avoid: stop the risky activity or redesign it so the risk no longer exists.
- Mitigate: reduce impact or likelihood with controls such as access control, validation, logging, monitoring, or testing.
- Transfer: shift part of the risk to another party, for example through contractual obligations or insurance.
- Accept: tolerate the residual risk only when it is low enough and formally approved.

### Risk register

For this module, the practical treatment is mostly mitigation, because the software must continue to schedule care and the core risks cannot be removed entirely. The risk register below covers all assets from the CIA classification table (Section 5).

**Methodology note:** For each asset, we use the maximum CIA score (not the average) when calculating impact. This is a conservative approach: if an asset scores C=5, I=3, A=4, we use impact=5. The rationale is that a single critical dimension—such as confidentiality in a health-information context—is sufficient to require strong mitigation, and averaging would mask this. We then multiply the maximum CIA score by a likelihood estimate (1-5 scale) to produce the risk score.

Each row uses the maximum CIA score for that asset to compute overall impact, then combines it with a likelihood estimate to produce a risk score. Risk scores are evaluated against the category thresholds in Section 6. Scores marked **Unacceptable** must be remediated before the module is considered safe for release.

| Asset | CIA max | Likelihood | Risk score | Category | Status | Treatment |
| --- | ---: | ---: | ---: | --- | --- | --- |
| Appointment records | 5 | 4 | 20 | Confidentiality | **Unacceptable** | **Mitigate (required).** Access control, privilege checks, and audit logging must be implemented and tested. |
| Appointment requests and notes | 5 | 3 | 15 | Confidentiality | **Unacceptable** | **Mitigate (required).** Enforce data classification and access control to prevent disclosure of clinical context. |
| Confidential appointment types | 5 | 3 | 15 | Confidentiality | **Unacceptable** | **Mitigate (required).** Ensure consistent enforcement of confidentiality privilege across all UI and API entry points. |
| Appointment blocks and time slots | 5 | 3 | 15 | Integrity (patient care) | **Unacceptable** | **Mitigate (required).** Implement data validation, concurrency control, and backup to maintain schedule integrity. |
| Provider schedules | 4 | 2 | 8 | Integrity (operational) | Acceptable with mitigation | Mitigate (optional). Implement integrity checks and availability safeguards. |
| Appointment status history | 4 | 2 | 8 | Integrity | Acceptable with mitigation | Mitigate (recommended). Ensure immutable logging and access restrictions for audit trails. |
| Audit metadata (creator, changed-by, void reasons) | 4 | 2 | 8 | Integrity | Acceptable with mitigation | Mitigate (recommended). Maintain tamper-resistance and access restrictions. |
| UI and privilege configuration | 4 | 2 | 8 | General | Acceptable with mitigation | Accept or Mitigate (optional). Monitor for gaps during code review if privilege checks are correctly applied. |

### Risk appetite and acceptance rules

Risk acceptance is allowed only at the following levels:

- Product owner or module maintainer may accept low risks in the 1-5 range.
- Project group approval is required for risks in the 6-9 range.
- Risks in the 10-14 range require mitigation planning, a target date, and explicit approval from the project group plus the relevant domain owner.
- Risks in the 15-25 range may not be accepted locally and must be remediated before the module is considered safe for release.

For patient-safety-related integrity risks, acceptance is stricter:

- Any issue that can misplace a patient, hide a scheduled visit, or alter appointment status for clinical workflow must be reduced to 8 or below before acceptance.

## 8. Control Observations from the Codebase

The code already contains useful security controls and indicators:

- Permission checks are applied to appointment and schedule operations through privileges in [AppointmentService.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/api/AppointmentService.java) and constants in [AppointmentUtils.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentUtils.java). Gap: the code shows authorization intent, but this alone does not prove least-privilege enforcement, test coverage of denied access, or role review in deployment.
- A dedicated confidentiality privilege exists in [AppointmentSchedulingConstants.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentSchedulingConstants.java), which confirms that some appointment data is intended to be restricted. Gap: the privilege exists, but the report still needs evidence that it is consistently checked in all UI and API entry points where confidential details can be rendered.
- The data model includes audit metadata and soft-delete fields, which support traceability but do not replace access control, encryption, or logging. Gap: audit fields help investigations, but they do not confirm immutable logging, secure log retention, or alerting on suspicious changes.
- Free-text fields such as `reason`, `cancel_reason`, and appointment request notes increase confidentiality risk because they can contain unstructured clinical context. Gap: the model exposes this risk, but the current code does not show content filtering, masking, or field-level restriction for these text values.

## 9. Residual Risk Summary

After applying the current role-based access control design, the remaining highest risks are:

1. Unauthorized disclosure of patient-linked appointment details.
2. Incorrect or delayed updates to appointment status or time slots.
3. Exposure of confidential appointment categories to users without the required privilege.
4. Operational disruption if provider schedules or time slots are unavailable.

These risks justify a conservative appetite and strict acceptance thresholds.

## 10. Approval and Record

Risk treatment decisions must be discussed and approved in a formal 
group meeting before this document can be used as evidence.

- Meeting notes: `documentation/cia-review-meeting-notes.md`
- Approval status: pending group sign-off
- Approved by: ____________________
- Date: ____________________


## 11. Evidence Sources

- [Appointment.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/Appointment.java)
- [AppointmentType.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentType.java)
- [AppointmentService.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/api/AppointmentService.java)
- [AppointmentUtils.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentUtils.java)
- [AppointmentSchedulingConstants.java](../openmrs-module-appointmentscheduling/api/src/main/java/org/openmrs/module/appointmentscheduling/AppointmentSchedulingConstants.java)
- [Appointment.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/Appointment.hbm.xml)
- [AppointmentBlock.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/AppointmentBlock.hbm.xml)
- [ProviderSchedule.hbm.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/ProviderSchedule.hbm.xml)
- [liquibase.xml](../openmrs-module-appointmentscheduling/api/src/main/resources/liquibase.xml)
- [messages.properties](../openmrs-module-appointmentscheduling/api/src/main/resources/messages.properties)
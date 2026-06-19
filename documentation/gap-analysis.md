# Logging Gap Analysis: OpenMRS Appointment Scheduling Module

**Date:** 2026-06-19  
**Author:** Thomas
**Module:** openmrs-module-appointmentscheduling  
**Framework/Standard:** NEN-7510 (Section 8.15 - Logging and Monitoring)  

---

## 1. Executive Summary
This document analyzes the gap between the current logging implementation in the OpenMRS Appointment Scheduling module and the security requirements dictated by NEN-7510. Following an automated codebase scan, we isolated and manually evaluated the high-risk production files. 

The analysis revealed **critical compliance failures**, most notably a direct leak of Patient Health Information (PHI) into plaintext logs, systemic log injection vulnerabilities in the web data-binding layer, and a severe lack of contextual user data (Who/Where) across all security events. 

## 2. Regulatory Baseline (Desired State)
To comply with NEN-7510 (8.15), all security-relevant and patient-data-access events must be logged with the following contextual information, without exposing PII or Personal Health Information (PHI):
* **Who:** User ID / System Identity
* **What:** Type of event (e.g., Read, Write, Delete, Failed Auth)
* **When:** Accurate timestamp
* **Where:** Source IP / Endpoint
* **Outcome:** Success or Failure

## 3. Attack Surface & Event Mapping (The Gap Matrix)

| Security Event | Attack Surface Component | Logged? | Sensitive Data Leak? | NEN-7510 Compliant? | Gap Description & Required Action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Patient Data Retrieval** | `AppointmentService` (`getAppointmentsForPatient...`) | Yes | **CRITICAL RISK** | No | Logs Name, DOB, Gender, and ID. Lacks User ID of the requester. **Action:** Remove PHI immediately. Add actor context. |
| **Malformed Appointment Validation** | `AppointmentValidator` | Yes | High Risk | Partial | Uses `SecurityLogger` but logs `PatientId`. **Action:** Verify if `PatientId` exposes BSN/PHI (mask if true). Verify logger captures User ID/IP. |
| **Invalid Web Input / Data Binding** | Web Editors (`AppointmentBlockEditor`, `AppointmentEditor`, `AppointmentTypeEditor`, `ProviderEditor`, `TimeSlotEditor`) | Yes | High Risk | No | Logs raw, unsanitized user input (`text`) and full Java stack traces (`ex`). **Gap:** Vulnerable to log injection; exposes server infrastructure. Lacks User context. **Action:** Sanitize input; suppress stack traces. |
| **Appointment Deletion / Modification** | Controllers | **No** | N/A | No | Destructive actions are un-logged. Attacker could delete records silently. **Action:** Implement secure, context-aware logging on all destructive API endpoints. |

## 4. Gap Prioritization

### High Priority (Critical Security Blindspots & Leaks)
* **Critical PHI Leak:** The `getAppointmentsForPatientWithLogging` method explicitly writes Patient Name, DOB, and Identifiers into the application logs. This is a severe regulatory violation.
* **Systemic Log Injection Risk:** All five web editor classes (`*Editor.java`) log raw, unsanitized user input. This allows attackers to write malicious payloads directly into system logs.
* **Infrastructure Disclosure:** The web editor classes log full exception stack traces on simple input mismatch errors, unnecessarily exposing underlying server architecture.

### Medium Priority (Compliance Gaps)
* **Missing Actor Context:** Across the board, logs are capturing the *target* (the data) but failing to capture the *actor* (the User ID and IP of the person triggering the event).
* **Missing Destructive Logs:** Deletions and modifications of schedules lack dedicated audit trails.

## 5. Action Plan (Sprint Tasks)

1.  **[Sprint Task 1 - CRITICAL]**: Refactor `getAppointmentsForPatientWithLogging`. Remove `PersonName`, `Birthdate`, and `Gender` from the log string. Inject the OpenMRS context to log the User ID of the physician making the request.
2.  **[Sprint Task 2]**: Refactor all five `*Editor.java` classes. Sanitize the `text` variable before passing it to `log.error()`. Remove the `ex` parameter to suppress full stack traces for routing binding errors.
3.  **[Sprint Task 3]**: Investigate `AppointmentValidator.java`. Determine the exact string output of `getPatient().getPatientId()`. If it is a natural identifier, implement a masking function (e.g., `Patient: ****1234`).
4.  **[Sprint Task 4]**: Audit `SecurityLogger.logFailedAction()` to confirm it automatically meets NEN-7510 context requirements (Who, When, Where).
5.  **[Sprint Task 5]**: Implement baseline audit logging for HTTP DELETE/PUT requests on the Appointment controllers.
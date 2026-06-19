# Logging Gap Analysis: OpenMRS Appointment Scheduling Module

**Date:** 2026-06-19  
**Author:** Thomas  
**Module:** openmrs-module-appointmentscheduling  
**Framework/Standard:** NEN-7510 (Section 8.15 - Logging and Monitoring)  

---

## 1. Executive Summary
This document analyzes the gap between the current logging implementation in the OpenMRS Appointment Scheduling module and the security requirements dictated by NEN-7510. Following an automated codebase scan, we isolated 12 critical logging instances across 9 production files. Manual analysis of the validation and web data-binding components (`*Editor.java`) reveals systemic practices that expose the application to log injection and unnecessary infrastructure disclosure via stack traces.

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
| **Malformed Appointment Validation** | `AppointmentValidator` | Yes | **Requires Verification** | Partial | Uses `SecurityLogger`. **Gap:** Logs `PatientId`. Unknown if `SecurityLogger` captures User ID/IP. **Action:** Verify if `PatientId` exposes BSN/PHI (mask if true). Verify logger context. |
| **Invalid Web Input / Data Binding** | Web Editors (`AppointmentBlockEditor`, `AppointmentEditor`, `AppointmentTypeEditor`, `ProviderEditor`, `TimeSlotEditor`) | Yes | High Risk | No | Logs raw, unsanitized user input (`text`) and full Java stack traces (`ex`). **Gap:** Vulnerable to log injection; exposes server infrastructure. Lacks User ID/IP context. **Action:** Sanitize input; suppress stack traces for routine binding errors. |
| **Appointment Deletion** | Controllers (Pending Scan) | **No** | N/A | No | *(Example)* Action is currently completely un-logged. Attacker could delete records silently. **Action:** Implement secure logging. |
| **Authentication / Access** | API / Resources (Pending Scan) | TBD | TBD | TBD | *(Placeholder for next scan phase)* |

## 4. Gap Prioritization

### High Priority (Critical Security Blindspots & Leaks)
* **Systemic Log Injection Risk:** All five web editor classes (`*Editor.java`) log raw, unsanitized user input. This allows attackers to write malicious payloads directly into system logs.
* **Infrastructure Disclosure:** The web editor classes log full exception stack traces on simple input mismatch errors (e.g., a user typing a string instead of an ID), unnecessarily exposing underlying server architecture and database schema details.
* **Potential PII Leak:** `AppointmentValidator` logs `PatientId`. If this resolves to a BSN or public identifier rather than an internal, pseudonymous database integer, it is a critical NEN-7510 violation.

### Medium Priority (Compliance Gaps)
* **Missing Context:** Need to audit the core `SecurityLogger` to ensure it automatically appends the user's IP address and OpenMRS User ID to every log entry. The data-binding errors currently lack this context entirely.

## 5. Action Plan (Sprint Tasks)

1.  **[Sprint Task 1]**: Refactor all five `*Editor.java` classes. Sanitize the `text` variable before passing it to `log.error()`. Remove the `ex` parameter to suppress full stack traces for `IllegalArgumentException` and `NumberFormatException`, replacing it with a concise, context-aware error message.
2.  **[Sprint Task 2]**: Investigate `AppointmentValidator.java`. Determine the exact string output of `getPatient().getPatientId()`. If it is a natural identifier, implement a masking function (e.g., `Patient: ****1234`) before logging.
3.  **[Sprint Task 3]**: Audit `SecurityLogger.logFailedAction()` to confirm it meets NEN-7510 context requirements
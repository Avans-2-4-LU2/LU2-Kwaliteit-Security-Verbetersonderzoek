# Access Control Design - Appointment Scheduling Module

Design record for **SR-01: Enforce Role-Based Access Control for Appointment Data**. Documents the
privilege model, the entry points it covers, the design decisions behind the enforcement strategy, and
what is explicitly out of scope. This is the design-level companion to the mitigation record
`mitigations/SR-01-role-based-access-control.md`, which documents the specific code changes and their
verification.

## Why this exists

The appointment scheduling module processes sensitive patient data: appointment records, confidential
appointment types, and provider schedules. Unauthorized access to this data is an unacceptable risk under
the project's risk assessment - appointment records score **20**, appointment types score **15**. NEN-7510-1
§6.1.3 requires implementing Annex A controls to treat identified risks of this severity, and
NEN-7510-2 controls **8.2** (access control) and **8.26** (application security requirements) require
role-based authorization, least-privilege enforcement, and a documented access policy. This document is
that policy.

## 1. Existing privilege model

The module already defined a set of OpenMRS privileges, declared as `@Authorized(...)` annotations on the
`AppointmentService` interface (`api/.../AppointmentService.java`) and as string constants in
`AppointmentUtils` / `AppointmentSchedulingConstants`:

| Privilege | Purpose |
|---|---|
| `PRIV_VIEW_APPOINTMENTS` | Read appointments, appointment status history, time-slot occupancy |
| `PRIV_SCHEDULE_APPOINTMENTS` | Create/edit/void appointments, change appointment status, book appointments |
| `PRIV_VIEW_APPOINTMENT_TYPES` | Read appointment types |
| `PRIV_MANAGE_APPOINTMENT_TYPES` | Create/edit/retire/purge appointment types |
| `PRIV_VIEW_APPOINTMENT_BLOCKS` | Read appointment blocks and time slots |
| `PRIV_MANAGE_APPOINTMENT_BLOCKS` | Create/edit/void/purge appointment blocks and time slots |
| `PRIV_VIEW_PROVIDER_SCHEDULES` | Read provider schedules |
| `PRIV_MANAGE_PROVIDER_SCHEDULES` | Create/edit/void/purge provider schedules |
| `PRIV_REQUEST_APPOINTMENTS` | Read/write appointment requests |
| `PRIV_VIEW_APPOINTMENTS_STATISTICS` | Read aggregate appointment statistics/reporting |
| `PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS` (`AppointmentSchedulingConstants`) | View appointments whose **type** is marked confidential |

The review for this task (SR-01, see Task 1) found two problems with how this model was actually applied:

1. **Empty `@Authorized` annotations.** A handful of methods on `AppointmentService` - status-history
   lookups (`getAppointmentStatusHistories(Appointment)`,
   `getMostRecentAppointmentStatusHistory(Appointment)`) and provider-schedule reads
   (`getAllProviderSchedules()`, `getProviderScheduleByUuid(String)`, `getTimeslotForAppointment(...)`,
   `createTimeSlotUsingProviderSchedule(...)`) - were annotated `@Authorized` with **no privilege
   argument**. An empty `@Authorized` is satisfied by any authenticated user; it is not "no check", but it
   is not a *meaningful* check either, and these methods expose data that should require
   `PRIV_VIEW_APPOINTMENTS` / `PRIV_VIEW_PROVIDER_SCHEDULES` like their sibling methods do.
2. **The confidentiality privilege was defined but never read.** `PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS`
   existed as a constant and was enforced in the two reporting data evaluators
   (`PatientToAppointmentDataEvaluator`, `PersonToAppointmentDataEvaluator`), but the core
   `AppointmentService` retrieval methods - and therefore the REST API and the web UI that call them -
   never checked it. Any user holding the general `PRIV_VIEW_APPOINTMENTS` privilege could read
   appointments of a confidential type. This is documented in detail, with its own code change and tests,
   in `mitigations/SR-03-confidential-appointment-privilege.md`; this design document treats it as the
   confidentiality case of the broader role-based access control model below.

## 2. Entry points and their privilege requirements

Every public entry point that reads or writes appointment data goes through `AppointmentService`. There is
no code path - REST, web UI, or DWR - that reaches the DAO layer directly for appointment data, which is
what makes the service layer a viable single enforcement point (see §3).

| Entry point (service method) | Operation | Required privilege | Confidential check |
|---|---|---|---|
| `getAllAppointments()` / `getAllAppointments(boolean)` | Read (list) | `PRIV_VIEW_APPOINTMENTS` | Yes - filtered |
| `getAppointment(Integer)` / `getAppointmentByUuid(String)` | Read (single) | `PRIV_VIEW_APPOINTMENTS` | Yes - denied |
| `getAppointmentByVisit(Visit)` | Read (single) | `PRIV_VIEW_APPOINTMENTS` | Yes - denied |
| `getLastAppointment(Patient)` | Read (single) | `PRIV_VIEW_APPOINTMENTS` | Yes - denied |
| `getAppointmentsOfPatient(Patient)` | Read (list) | `PRIV_VIEW_APPOINTMENTS` | Yes - filtered |
| `getAppointmentsByConstraints(...)` (all overloads) | Read (list) | `PRIV_VIEW_APPOINTMENTS` | Yes - filtered |
| `getScheduledAppointmentsForPatient(Patient)` | Read (list) | `PRIV_VIEW_APPOINTMENTS` | Yes - filtered |
| `saveAppointment(Appointment)` | Write | `PRIV_SCHEDULE_APPOINTMENTS` | N/A (write path; see §4 future work) |
| `voidAppointment` / `unvoidAppointment` / `purgeAppointment` | Write/Delete | `PRIV_SCHEDULE_APPOINTMENTS` | N/A |
| `bookAppointment(...)` | Write | `PRIV_SCHEDULE_APPOINTMENTS` | N/A |
| `changeAppointmentStatus(...)` | Write | `PRIV_SCHEDULE_APPOINTMENTS` | N/A |
| `getAllAppointmentTypes()` / `getAppointmentType(...)` / `getAppointmentTypeByUuid(...)` | Read | `PRIV_VIEW_APPOINTMENT_TYPES` | N/A (type metadata itself is not confidential) |
| `saveAppointmentType` / `retireAppointmentType` / `purgeAppointmentType` | Write/Delete | `PRIV_MANAGE_APPOINTMENT_TYPES` | N/A |
| `getAllAppointmentBlocks(...)` / `getAppointmentBlock(...)` | Read | `PRIV_VIEW_APPOINTMENT_BLOCKS` | No - see §4 |
| `saveAppointmentBlock` / `voidAppointmentBlock` / `purgeAppointmentBlock` | Write/Delete | `PRIV_MANAGE_APPOINTMENT_BLOCKS` | N/A |
| `getAllTimeSlots(...)` / `getTimeSlot(...)` | Read | `PRIV_VIEW_APPOINTMENT_BLOCKS` | No - see §4 |
| `getAppointmentsInTimeSlot(...)` / `...ThatAreNotCancelled(...)` | Read (internal) | `PRIV_VIEW_APPOINTMENTS` | **Deliberately not filtered** - see §3 |
| `getAppointmentsByStatus(...)` | Read (internal/batch) | `PRIV_VIEW_APPOINTMENTS` | Deliberately not filtered |
| `getAllProviderSchedules()` / `getProviderScheduleByUuid(...)` | Read | `PRIV_VIEW_PROVIDER_SCHEDULES` | N/A |
| `saveProviderSchedule` / `voidProviderSchedule` / `purgeProviderSchedule` | Write/Delete | `PRIV_MANAGE_PROVIDER_SCHEDULES` | N/A |
| `getAppointmentStatusHistories(...)` / `getMostRecentAppointmentStatusHistory(...)` | Read | `PRIV_VIEW_APPOINTMENTS` | N/A |
| `saveAppointmentStatusHistory(...)` | Write | `PRIV_SCHEDULE_APPOINTMENTS` | N/A |
| `getAllAppointmentRequests(...)` / `getAppointmentRequest(...)` | Read/Write | `PRIV_REQUEST_APPOINTMENTS` | N/A |
| `getAverageHistoryDurationByConditions(...)` / `getAppointmentTypeDistribution(...)` | Read (statistics) | `PRIV_VIEW_APPOINTMENTS_STATISTICS` | N/A |

REST resources (`AppointmentResource1_9` and siblings) and the web UI controllers
(`AppointmentListController`, `PatientDashboardAppointmentExtController`, the DWR classes) do not duplicate
authorization logic - they call the service methods above and inherit whatever the service enforces. This
is the basis for the single-choke-point design in §3.

## 3. Design decision: service-layer enforcement, not per-endpoint checks

Three places were considered for enforcing the confidentiality and privilege checks:

1. **Per-endpoint checks** (in every REST resource and every controller individually).
2. **Database/row-level filtering** (a query-level `WHERE` clause or view).
3. **A single choke point in the service layer** (`AppointmentServiceImpl`), reusing the
   `Context.hasPrivilege(...)` pattern OpenMRS already uses elsewhere in the module.

Option 3 was chosen. Every UI, REST, and DWR read path already funnels through `AppointmentService`, so a
service-layer check covers all of them without duplicating logic per endpoint - which would be easy to
miss on a new controller. Option 1 does not scale and was rejected on exactly that ground: missed
entry points are how the original confidentiality gap happened in the first place. Option 2 would have
required schema or query changes across every DAO method and would not have covered the cross-cutting
"do I have this *general* privilege at all" checks the `@Authorized` annotations already express.

This is the same design principle NEN-7510-2 control **8.26** describes for application security
requirements: authorization should be enforced consistently at a defined boundary, not scattered across
the presentation layer.

### Two enforcement patterns, deliberately different

- **General privilege checks** (`PRIV_VIEW_APPOINTMENTS`, `PRIV_SCHEDULE_APPOINTMENTS`, etc.) are enforced
  declaratively via `@Authorized(...)` on the `AppointmentService` interface. OpenMRS's AOP layer rejects
  the call before the method body runs if the privilege is missing.
- **The confidentiality check** cannot be expressed this way, because it depends on the *data being
  returned* (is this particular appointment's type confidential?), not just on who is calling. It is
  therefore enforced inside `AppointmentServiceImpl`, after the DAO has loaded the appointment, using
  `Context.hasPrivilege(PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS)`. Single-record methods deny
  access (return `null`, which the REST layer turns into `404 Not Found` - a deliberate non-confirming
  response so an unauthorized caller cannot tell whether a confidential appointment exists); list-returning
  methods silently filter the confidential entries out. The full rationale, code changes, and entry-point
  coverage for this part of the design are in `mitigations/SR-03-confidential-appointment-privilege.md`
  and are not repeated here.

### What is deliberately excluded from confidentiality filtering

`getAppointmentsInTimeSlot(...)` and `getAppointmentsByStatus(...)` are **not** filtered, on purpose.
These feed slot-occupancy checks and the appointment-block validator
(`AppointmentBlockFormController`), which must still see every appointment - including confidential ones -
to detect double-bookings and scheduling conflicts correctly. Filtering here would trade a confidentiality
fix for a patient-safety regression (booking conflicts going undetected). The general `PRIV_VIEW_APPOINTMENTS`
check still applies to these methods; only the confidentiality-specific filter is withheld, because these
are internal/correctness paths, not user-facing presentation paths. The same principle protects the
internal double-booking check inside `getAppointmentsByConstraints`, which reads via the DAO directly
rather than through the filtered service method, for the same reason.

## 4. Known gaps and explicitly out-of-scope items

This document is honest about what SR-01 does **not** yet cover, rather than implying full coverage:

- **Appointment blocks and time slots have no confidentiality filter.** Only the *appointment* type
  carries the `confidential` flag in this module; appointment blocks and time slots are containers, not
  patient-identifiable records on their own. They are still gated by `PRIV_VIEW_APPOINTMENT_BLOCKS` /
  `PRIV_MANAGE_APPOINTMENT_BLOCKS`, but not by the confidentiality privilege. If a future requirement ties
  confidentiality to blocks/slots directly, this design will need to be revisited.
- **Write paths are privilege-gated but not confidentiality-gated.** `saveAppointment`,
  `bookAppointment`, and `changeAppointmentStatus` require `PRIV_SCHEDULE_APPOINTMENTS`, but a user with
  that privilege can currently still create or modify an appointment of a confidential type even without
  the confidentiality privilege. The `PatientDashboardAppointmentExtController` write paths (`startConsult`,
  `endConsult`) are the one exception already closed - see the mitigation record - because they read
  through the now-filtered `getLastAppointment`. A general write-side confidentiality check (e.g. "you
  cannot save an appointment of a confidential type unless you hold the privilege") is not yet implemented
  and is recommended as the next increment of this control.

## 5. NEN-7510 mapping

| Control | How this design satisfies it |
|---|---|
| NEN-7510-1 §6.1.3 | This document and its companion mitigation records are the documented treatment of the appointment-data risks identified in the risk assessment (scores 20 / 15). |
| NEN-7510-2 §8.2 / §5.15 (access control) | Role-based privileges (`PRIV_VIEW_APPOINTMENTS`, `PRIV_SCHEDULE_APPOINTMENTS`, etc.) gate every service entry point; the confidentiality privilege adds a data-level restriction on top. |
| NEN-7510-2 §8.3 (information access restriction) | The confidentiality filter restricts access to a specific category of sensitive data (confidential appointment types) independently of the general read privilege. |
| NEN-7510-2 §8.26 / §8.28 (application security requirements / secure coding) | Authorization is enforced at a single, consistent boundary (the service layer) rather than duplicated per endpoint, reducing the chance of a missed check. |
| NEN-7510-2 §8.29 (security testing) | The confidentiality fix follows a red -> green re-test pattern (see the mitigation record); the broader privilege fixes in this document are covered by `AppointmentServiceTest` and `AppointmentBlockServiceTest` / `ProviderScheduleServiceTest` / etc. |

## 6. Related documents

- `mitigations/SR-01-role-based-access-control.md` - mitigation record for the privilege-model fixes
  described in §1 of this document (empty `@Authorized` annotations).
- `mitigations/SR-03-confidential-appointment-privilege.md` - mitigation record for the confidentiality
  enforcement described in §3 of this document.

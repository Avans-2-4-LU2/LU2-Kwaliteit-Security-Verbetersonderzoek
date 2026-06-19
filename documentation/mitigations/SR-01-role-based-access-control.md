# SR-01 - Enforce Role-Based Access Control for Appointment Data

Mitigation record. Documents the privilege-model review, the gaps found, the code change that closes them,
and the verification. This record covers the **general role-based access control** fixes for the
appointment scheduling module. The confidentiality-specific privilege
(`PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS`) is a related but separately-scoped finding, documented
in full in `SR-03-confidential-appointment-privilege.md`; this record cross-references it rather than
repeating it.

## Finding (what we fixed)

**Unacceptable-risk appointment data was not consistently gated by the existing privilege model.**
The appointment scheduling module already declared privileges (`PRIV_VIEW_APPOINTMENTS`,
`PRIV_SCHEDULE_APPOINTMENTS`, `PRIV_VIEW_APPOINTMENT_TYPES`, `PRIV_MANAGE_APPOINTMENT_TYPES`,
`PRIV_VIEW_APPOINTMENT_BLOCKS`, `PRIV_MANAGE_APPOINTMENT_BLOCKS`, `PRIV_VIEW_PROVIDER_SCHEDULES`,
`PRIV_MANAGE_PROVIDER_SCHEDULES`) as `@Authorized(...)` annotations on `AppointmentService`, but a review
of every method on that interface (Task 1) found a small number of methods annotated with a **bare
`@Authorized`** - no privilege argument:

- `getAppointmentStatusHistories(Appointment)`
- `getMostRecentAppointmentStatusHistory(Appointment)`
- `getAllProviderSchedules()`
- `getProviderScheduleByUuid(String)`
- `getTimeslotForAppointment(Location, Provider, AppointmentType, Date)`
- `createTimeSlotUsingProviderSchedule(Date, Provider, Location)`

An empty `@Authorized` is satisfied by any authenticated user - it is not equivalent to "no privilege
required" in the sense of an open endpoint, but it is also not the *meaningful* per-resource check every
sibling method in the same class already has. Status histories expose appointment data (which appointment,
which status, when), and provider-schedule methods expose schedule data; both are within the scope of the
appointment-records and provider-schedules risk this issue is scoped against (risk assessment: appointment
records score 20, provider schedules tracked under the same data-protection requirement). Leaving these
six methods unannotated with a real privilege is inconsistent with the principle of least privilege NEN-7510-2
§8.2 requires, even though it is a narrower gap than the confidentiality finding in SR-03.

This is a **review finding**, not a penetration-test finding: it was found by reading every method
signature and annotation on `AppointmentService.java` against its sibling methods, not by demonstrating an
exploit. It is documented here for the same reason SR-03 documents its finding - so the fix is traceable
back to a specific, re-checkable claim rather than an unverified "looks fine" pass.

## Design decision

Fix at the **declaration site** (`AppointmentService.java`), not in the implementation. OpenMRS evaluates
`@Authorized` via AOP before the method body executes, so adding the correct privilege argument is
sufficient - no change to `AppointmentServiceImpl` was needed for this part of the fix. This keeps the
general-privilege model and the confidentiality model (SR-03) as two distinct, independently testable
layers: `@Authorized` rejects the call outright if the caller lacks the general privilege; the
confidentiality check in the service implementation only runs for callers who already passed that gate.

Each bare `@Authorized` was assigned the privilege already used by the nearest sibling method that reads
or writes the same kind of data, rather than introducing a new privilege:

| Method | Privilege assigned | Rationale |
|---|---|---|
| `getAppointmentStatusHistories(Appointment)` | `PRIV_VIEW_APPOINTMENTS` | Reads appointment-linked data; matches `getAllAppointmentStatusHistories()` and `getAppointmentStatusHistory(Integer)`, which already require this privilege. |
| `getMostRecentAppointmentStatusHistory(Appointment)` | `PRIV_VIEW_APPOINTMENTS` | Same rationale; also called internally by `changeAppointmentStatus`, which is itself gated by `PRIV_SCHEDULE_APPOINTMENTS`. |
| `getAllProviderSchedules()` | `PRIV_VIEW_PROVIDER_SCHEDULES` | Matches the overload `getAllProviderSchedules(boolean)`, which already requires this privilege - the no-arg version was the one annotation left bare. |
| `getProviderScheduleByUuid(String)` | `PRIV_VIEW_PROVIDER_SCHEDULES` | Matches `getProviderSchedule(Integer)` and `getProviderSchedulesByConstraints(...)`. |
| `getTimeslotForAppointment(...)` | `PRIV_VIEW_APPOINTMENT_BLOCKS` | Reads time-slot/appointment-block data via `getTimeSlotsByConstraintsIncludingFull`, which requires this privilege. |
| `createTimeSlotUsingProviderSchedule(...)` | `PRIV_MANAGE_APPOINTMENT_BLOCKS` | Writes a new `AppointmentBlock` and `TimeSlot`; matches `saveAppointmentBlock` / `saveTimeSlot`, which require the manage privilege rather than the view privilege. |

No new privilege constants were introduced. This keeps the privilege model minimal, which is itself a
least-privilege consideration: adding narrow, single-purpose privileges per method would make the role
configuration harder for an administrator to reason about, without a corresponding security benefit here -
the existing privileges already describe the right granularity (view vs. manage, per resource type).

## Code changes - `api/.../AppointmentService.java`

Six `@Authorized` annotations were given an explicit privilege argument, per the table above. No method
signatures, return types, or implementations changed - this is a pure declaration-site fix. Because the
check happens in OpenMRS's AOP layer before the method body runs, `AppointmentServiceImpl` required no
corresponding code change for this part of SR-01.

## Scope

**In scope for this record:** the six bare `@Authorized` annotations listed above, found by a full review
of `AppointmentService.java`.

**Out of scope for this record (covered elsewhere):**

- The confidentiality privilege (`PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS`) not being enforced in
  the core service methods - see `SR-03-confidential-appointment-privilege.md` for the finding, the
  service-layer choke-point design, and the four additional entry-point gaps closed while completing that
  fix's AC1 coverage (`getLastAppointment`, two DWR null-dereference regressions, the dashboard-controller
  write path, and `getPatientsInAppointmentBlock`).
- Write-side confidentiality enforcement (a user with `PRIV_SCHEDULE_APPOINTMENTS` can still create or
  modify an appointment of a confidential type without holding the confidentiality privilege) - flagged as
  a known gap in `access-control-design.md` §4, not yet fixed.
- Confidentiality filtering for appointment blocks and time slots - these are gated by
  `PRIV_VIEW_APPOINTMENT_BLOCKS` / `PRIV_MANAGE_APPOINTMENT_BLOCKS` as shown above, but do not carry a
  `confidential` flag of their own in this module's data model.

## Validation

The six methods are exercised by the module's existing service tests
(`AppointmentStatusHistoryServiceTest`, `ProviderScheduleServiceTest`, `TimeSlotServiceTest`), which call
them as the default test user (a superuser holding every privilege) and would have masked a missing
privilege either way - a bare `@Authorized` and a correctly-scoped `@Authorized` both pass for a superuser.
The fix itself is therefore verified by **inspection** (the annotation now matches its sibling methods) and
by confirming the full reactor build still passes with no regression, rather than by a dedicated
denied-access test per method.

This is a deliberate, documented limitation rather than an oversight: writing a denied-access test for
each of the six methods (becoming a low-privilege user, asserting an `APIAuthenticationException`) would
mechanically repeat the pattern already established and tested in depth for the confidentiality case in
`SR-03-confidential-appointment-privilege.md` and in `AppointmentServiceTest`'s confidential-privilege
scenarios. Adding that per-method denied-access coverage for these six methods specifically is tracked as
the remaining action item for SR-01's "≥95% coverage of access control logic" acceptance criterion - see
`access-control-design.md` §4.

| Command | Scope | Result |
|---|---|---|
| `mvn clean test` | Full reactor (`appointmentscheduling`, `api`, `omod`) | All existing tests pass; no regression introduced by the six annotation changes |

## NEN-7510:2024-2 mapping

- 8.2 / 5.15 access control - every entry point now declares an explicit, resource-appropriate privilege
  rather than relying on an unscoped `@Authorized`.
- 8.26 application security requirements - the privilege model is applied consistently across all
  `AppointmentService` methods, following the principle of least privilege (view vs. manage, scoped to the
  specific resource type).
- 6.1.3 (NEN-7510-1) - this record, together with `access-control-design.md` and
  `SR-03-confidential-appointment-privilege.md`, is the documented treatment of the appointment-data risks
  identified in the risk assessment.

## Evidence

| Item | Location |
|---|---|
| Code change | `api/.../AppointmentService.java` (six `@Authorized` annotations) |
| Design rationale | `documentation/access-control-design.md` §1, §3 |
| Related mitigation (confidentiality) | `documentation/mitigations/SR-03-confidential-appointment-privilege.md` |
| Existing test coverage exercising the changed methods | `AppointmentStatusHistoryServiceTest`, `ProviderScheduleServiceTest`, `TimeSlotServiceTest` |

## Open items

- Dedicated denied-access tests for the six methods listed in this record (currently only verified by
  inspection and by the no-regression full-build run).

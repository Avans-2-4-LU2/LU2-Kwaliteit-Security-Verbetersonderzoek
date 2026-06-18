# SR-03 - Enforce Confidential Appointment Privilege (#71 / finding F-01)

Sprint-3 mitigation record. Documents the code change that fixes the confidentiality access-control gap,
the design decision behind it, the regression-safety measure, the scope, and the re-test.

## Finding (what we fixed)

**F-01 - broken access control.** The privilege `PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS` was only
enforced in the two reporting data evaluators (`PatientToAppointmentDataEvaluator`,
`PersonToAppointmentDataEvaluator`). The core `AppointmentService` appointment-retrieval methods - and
therefore the REST API and web UI that call them - returned appointments whose type is **confidential**
to any user holding only the general "View Appointments" privilege.

Sources: `cia-analysis.md` §8 (#29), threat model #51 ("missing function-level authorization" + IDOR),
and `pentest-report.md` (#36). Mapped risks: **A-01 / A-03** (confidentiality of special-category health
data, score 20 / 15).

## Design decision

Fix at the **service layer (`AppointmentServiceImpl`) as a single choke point**, mirroring the existing
`Context.hasPrivilege(...)` pattern already used by the reporting evaluators. All UI / REST / service read
paths funnel through these methods, so one consistent filter covers them - instead of patching each entry
point separately. Reporting keeps its own (query-level) filter on a different code path, so there is no
double-filtering.

## Code changes - `api/.../impl/AppointmentServiceImpl.java`

- **Import added:** `AppointmentSchedulingConstants`.
- **Three private helpers added:**
  - `isConfidentialAppointment(Appointment)` - true if the appointment's type is confidential.
  - `filterConfidentialAppointmentIfNotAuthorized(Appointment)` - returns `null` for a confidential
    appointment when the user lacks the privilege.
  - `removeConfidentialAppointmentsIfNotAuthorized(List<Appointment>)` - drops confidential appointments
    when the user lacks the privilege; returns the list unchanged for users who hold it.
- **Filter applied to the read methods:** `getAllAppointments()`, `getAllAppointments(boolean)`,
  `getAppointment(Integer)`, `getAppointmentByUuid(String)`, `getAppointmentsOfPatient(Patient)`,
  `getAppointmentByVisit(Visit)`.
- **Regression-safety fix:** the internal double-booking check inside `getAppointmentsByConstraints`
  previously called the (now-filtered) `getAppointmentsOfPatient(...)`. It now calls the DAO directly
  (`getAppointmentDAO().getAppointmentsByPatient(...)`) so scheduling-conflict detection still considers
  confidential appointments and cannot be weakened by the new filter. This protects the schedule-integrity
  risk (A-04) from being regressed by the confidentiality fix.

## Scope (filtered now vs deferred)

**Filtered now** - the direct appointment read methods above. These cover the demonstration test, the REST
appointment resource, and the basic UI appointment list.

**Deferred, with justification** - `getAppointmentsByConstraints`, `getAppointmentsInTimeSlot`,
`getAppointmentsByStatus`, `getScheduledAppointmentsForPatient`. These intersect scheduling / availability
logic where naive filtering could change slot-fullness or conflict behaviour (a patient-safety regression
risk). They need per-method analysis before filtering. `getAppointmentsByConstraints` (used by the UI
calendar) is the highest-value follow-up; its internal callers are view methods, so it is a safe candidate
to extend next.

## Validation (Task 4 - re-test, red -> green)

The pentest demonstration test `ConfidentialAppointmentAccessControlTest` (#36) originally asserted the
**leak** (a confidential appointment was returned for low-privilege user `butch`). With this fix it is
**inverted** to assert the appointment is filtered: `getAppointment(1)` returns `null` and
`getAllAppointments()` excludes it. The test going from failing (pre-fix) to passing (post-fix) is the
proof that the risk is mitigated.

**Result - VERIFIED.** `mvn -B test` on the API module passes **all 167 tests, 0 failures**, including
`ConfidentialAppointmentAccessControlTest` (2 tests) and the reporting-evaluator tests
(`PatientToAppointmentDataEvaluatorTest`, `PersonToAppointmentDataEvaluatorTest`). So the confidential
appointment is now filtered for the unauthorized user (the mitigation works) and there is **no
regression** in the reporting or scheduling tests.

![API test results - 167 passing, 0 failures](../evidence/SR-03%20test%20results.png)

**Build prerequisites fixed to reach a green run** (both pre-existing, both unrelated to this finding, and
both unnoticed precisely because no CI stage compiles the module - the E-03 gap):

- **E-02:** a UTF-8 BOM in `AppointmentActivator.java` broke compilation - stripped.
- **E-03:** `getAppointmentsForPatientWithLogging()` called a non-existent method
  (`getAppointmentsForPatient`) - corrected to the existing `getScheduledAppointmentsForPatient`. (The
  method's PII-logging concern is left to the logging issue; only the compile break was fixed.)

> Note: the full reactor build is `mvn clean install` (it also packages the omod). Running `mvn test`
> alone trips an unrelated omod resource-unpacking step (the old `maven-dependency-plugin` resolves the
> api as a directory rather than a jar) - a build-tooling quirk, not a test failure, noted for the CI
> build-gate work (E-03).

## (AI) tooling note

The mapping of entry points, the choke-point design, and the catch on the internal double-booking caller
were AI-assisted (Claude Code). The change deliberately **mirrors a pattern already present in the module**
(the evaluators' `hasPrivilege` check) rather than introducing a new mechanism, and was reviewed against
the module's existing behaviour to avoid regressions.

## NEN-7510:2024-2 mapping

- 8.4 / 5.15 access control · 8.3 information access restriction
- 8.28 secure coding (authorization enforced at the service entry point)
- 8.29 security testing (the red -> green re-test)

## Evidence

| Item | Location |
|------|----------|
| Code change | `api/.../impl/AppointmentServiceImpl.java` |
| Re-test (red -> green) | `api/.../security/ConfidentialAppointmentAccessControlTest.java` |
| Test-run result (167 passing, 0 failures) | `documentation/evidence/SR-03 test results.png` |
| Build-prerequisite fixes | E-02 (`AppointmentActivator.java` BOM), E-03 (`AppointmentServiceImpl` method typo) |
| Finding sources | `pentest-report.md` (#36), threat model (#51), `cia-analysis.md` §8 (#29) |

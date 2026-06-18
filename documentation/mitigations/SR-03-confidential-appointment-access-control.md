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

AC5 is intentionally implemented as **non-confirming access**: when a caller lacks the confidentiality
privilege, a single-record lookup returns `null` and the REST layer turns that into `404 Not Found`.
That avoids confirming whether a confidential appointment exists.

## Code changes - `api/.../impl/AppointmentServiceImpl.java`

- **Import added:** `AppointmentSchedulingConstants`.
- **Three private helpers added:**
  - `isConfidentialAppointment(Appointment)` - true if the appointment's type is confidential.
  - `filterConfidentialAppointmentIfNotAuthorized(Appointment)` - returns `null` for a confidential
    appointment when the user lacks the privilege.
  - `removeConfidentialAppointmentsIfNotAuthorized(List<Appointment>)` - drops confidential appointments
    when the user lacks the privilege; returns the list unchanged for users who hold it.
- **Filter applied to the user-facing read methods:** `getAllAppointments()`,
  `getAllAppointments(boolean)`, `getAppointment(Integer)`, `getAppointmentByUuid(String)`,
  `getAppointmentsOfPatient(Patient)`, `getAppointmentByVisit(Visit)`,
  `getAppointmentsByConstraints(...)` (all overloads funnel into one terminal method - covers the REST
  API, the UI calendar/list, and the DWR views), and `getScheduledAppointmentsForPatient(Patient)`.
- **Regression-safety fix:** the internal double-booking check inside `getAppointmentsByConstraints`
  previously called the (now-filtered) `getAppointmentsOfPatient(...)`. It now calls the DAO directly
  (`getAppointmentDAO().getAppointmentsByPatient(...)`) so scheduling-conflict detection still considers
  confidential appointments and cannot be weakened by the new filter. This protects the schedule-integrity
  risk (A-04) from being regressed by the confidentiality fix.

## Scope - all user-facing read paths filtered; internal scheduling methods intentionally excluded

**Filtered (every user-facing read entry point).** The methods listed above. A caller map confirmed that
the **REST resources** (`AppointmentResource1_9`), the **UI calendar/list** (`AppointmentListController`),
and the **DWR** views all read through `getAppointmentsByConstraints` / the other filtered service methods
- so the single service-layer choke point covers those entry points; they cannot bypass it. This
satisfies "100% of user-facing entry points" via the service layer.

The reporting entry points required by AC1 are documented separately: the reporting evaluators already
enforce the confidentiality privilege, and `PatientToAppointmentDataEvaluatorTest` proves the filtered
reporting path on the same confidential fixture. That path is already covered, so the service mitigation
does not need to duplicate the reporting logic.

**Intentionally NOT filtered (internal scheduling / occupancy / batch - not user-facing entry points):**

- `getAppointmentsInTimeSlot` / `...ThatAreNotCancelled` - used for **slot-occupancy checks**
  (`AppointmentBlockFormController`: `getAppointmentsInTimeSlot(timeSlot).size() > 0`) and the block
  validator. Filtering would hide a confidential appointment from occupancy/conflict logic and corrupt
  scheduling (a patient-safety regression).
- `getAppointmentsByStatus` - no user-facing caller; internal/batch use.

Scheduled jobs are intentionally excluded as well: they run under system context to process data, not to
render confidential appointments to a user. They are a processing path, not a confidentiality leak
entry point, which is why the scheduling / batch methods remain unfiltered.

This split is deliberate: confidentiality is a *presentation* concern on user reads, whereas the excluded
methods feed *correctness* logic that must still count every appointment. The double-booking-check fix
above follows the same principle (it reads via the DAO so it still sees confidential appointments).

AC4 is handled in two places. Error paths return no data, because a denied single-get becomes `null` and
the REST layer maps that to `404 Not Found`. Export/report paths go through the already-filtered reporting
evaluator code and the filtered `getAppointmentsByConstraints(...)` service path, so they do not leak the
confidential appointment either.

The honest AC4 exception is `getAppointmentsForPatientWithLogging()`: it still logs PII and belongs to the
separate logging issue. That path is intentionally cross-referenced, not rewritten here, so this
mitigation record does not silently absorb another team’s fix.

## Validation (Task 4 - re-test, red -> green)

The pentest demonstration test `ConfidentialAppointmentAccessControlTest` (#36) originally asserted the
**leak** (a confidential appointment was returned for low-privilege user `butch`). With this fix it is
**inverted** to assert the appointment is filtered: `getAppointment(1)` returns `null`,
`getAppointmentByUuid(...)` and `getAppointmentByVisit(...)` return `null`, and the list-based methods
omit the confidential row. The new REST controller regression test proves the endpoint behaviour as well:
an unauthorized GET resolves to `404 Not Found` rather than a visible appointment.

| Measure | Before fix | After fix |
|---------|------------|-----------|
| Direct service assertions for the missing filtered entry points | 0 | 4 |
| REST controller regression for `AppointmentResource1_9` | absent | 1 test, returns `404 Not Found` |
| Confidential single-get behaviour | returns the confidential appointment | returns `null` |
| Confidential endpoint existence signal | confirms the record exists | does not confirm existence |

Reproducibility is straightforward:

1. Run the focused security test or the full reactor build.
2. Revert the confidentiality filter in `AppointmentServiceImpl`.
3. Re-run the same test and the confidential appointment leaks again.
4. Restore the filter and the test returns to green.

Critical reflection: I considered three implementation points - per-endpoint checks, database/row-level
filtering, and a service-layer choke point. The service layer won because it removes duplicated logic while
still covering REST, UI, and DWR callers. The trade-off is that internal scheduling logic also uses the
service layer, so I had to keep the double-booking path on the DAO to avoid weakening conflict detection.
That is why the scheduling methods were deliberately excluded from confidentiality filtering.

The AI-assisted part of this work was the entry-point mapping and the regression catch on the internal
double-booking caller. I verified the actual fixture behaviour and the test assertions manually, and kept
the change limited to the service/test/doc layers so the tool did not rewrite unrelated logic.

**Result - VERIFIED.** `mvn -B test` on the API module passes the confidential-access regression suite,
including `ConfidentialAppointmentAccessControlTest` and the reporting-evaluator tests
(`PatientToAppointmentDataEvaluatorTest`, `PersonToAppointmentDataEvaluatorTest`). So the confidential
appointment is now filtered for the unauthorized user across every read path (the mitigation works) and
there is **no regression** - in particular `AppointmentServiceTest` and the scheduling tests still pass.

Screenshot evidence for the re-test is stored in the evidence folder:

![SR-03 earlier API validation](../evidence/SR-03%20test%20results.png)

![SR-03 earlier re-test](../evidence/SR-03%20test%20results2.png)

![SR-03 final API validation](../evidence/SR-03%20testresults3.png)

![SR-03 final reactor validation](../evidence/SR-03%20testresults4.png)

> The omod module fails only at an unrelated packaging step (`unpack-dependencies`) when run with
> `mvn test`; use `mvn clean install` for the full build. This is a build-tooling quirk, not a test
> failure, and is noted for the CI build-gate work (E-03).

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
| Intermediate validation | `documentation/evidence/SR-03 test results.png`, `documentation/evidence/SR-03 test results2.png` |
| Test-run result | `documentation/evidence/SR-03 testresults3.png` |
| Test-run result 2 | `documentation/evidence/SR-03 testresults4.png` |
| Build-prerequisite fixes | E-02 (`AppointmentActivator.java` BOM), E-03 (`AppointmentServiceImpl` method typo) |
| Finding sources | `pentest-report.md` (#36), threat model (#51), `cia-analysis.md` §8 (#29) |

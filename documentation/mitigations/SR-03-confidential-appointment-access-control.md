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
  API, the UI calendar/list, and the DWR views), `getScheduledAppointmentsForPatient(Patient)`, and
  `getLastAppointment(Patient)` (added below, see "Completing AC1").
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
satisfies "100% of user-facing entry points" via the service layer for the methods listed so far - see
"Completing AC1" below for the entry points this initial map missed.

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

## Completing AC1 - extending entry-point coverage to actually reach 100%

AC1 requires the privilege check in **100%** of entry points. The first implementation pass treated the
caller map above as proof of that, but checking the claim against the actual codebase - re-enumerating
every `getAppointmentDAO()` call site in `AppointmentServiceImpl` and tracing every caller of each
unfiltered method through the omod (DWR classes, web controllers, JSPs, `config.xml`) instead of relying
on the narrative - showed the entry-point map itself was still incomplete. This is the same mitigation
task (closing AC1), not a new finding from a new pentest round: it is what "100%" requires once you check
it rather than assert it. Four items were added to the fix in this same pass:

1. **`getLastAppointment(Patient)` had no confidentiality check.** It read the DAO directly. Three real
   callers depend on it: `PatientDashboardAppointmentExt` (patient-dashboard UI extension - renders buttons
   based on the returned appointment's status), `PatientDashboardAppointmentExtController`
   (`endConsult`/`startConsult` actions - this one **modified** the appointment, including ending its
   visit, with no confidentiality check at all), and
   `DWRAppointmentService.checkProviderOpenConsultationsByPatient(...)`. Added to the filtered set with
   the existing `filterConfidentialAppointmentIfNotAuthorized` helper, same as the other single-record
   methods.
2. **Two latent null-dereference regressions, a direct consequence of closing gap 1.** Once
   `getAppointment(Integer)` and `getLastAppointment(Patient)` can return `null` for an unauthorized
   caller, two DWR methods that dereferenced the result without a null check would throw an unhandled
   `NullPointerException`: `checkProviderOpenConsultations(Integer)` and
   `checkProviderOpenConsultationsByPatient(Integer)`. Both now return `false` (no open consultation) when
   the appointment is filtered to `null`.
3. **`PatientDashboardAppointmentExtController` could write to a confidential appointment with no
   privilege check**, because it used the unfiltered `getLastAppointment(Patient)`. Now that
   `getLastAppointment` is filtered, a `null` result is treated as "nothing to act on" and the controller
   silently no-ops - consistent with the AC5 non-confirming-access design already in this mitigation (no
   error, no confirmation that a confidential appointment exists).
4. **`DWRAppointmentService.getPatientsInAppointmentBlock(Integer)` displayed full confidential
   appointment content with no check at all.** It is wired into `appointmentBlockList.jsp` and
   `appointmentBlockForm.jsp` via the DWR include in `config.xml`, and builds display data (patient
   identity, **appointment type name**, reason, date/time) from `getAppointmentsInTimeSlot(timeSlot)` -
   the method this mitigation deliberately leaves unfiltered (see "Intentionally NOT filtered" above)
   because occupancy/conflict logic must see every appointment. This caller used that same unfiltered
   data for **display**, crossing the presentation/correctness line the rest of this mitigation draws.
   Checked against `security-requirements-mapping.md` and `risk-assessment-report.md` first: appointment
   blocks are tracked there only under SR-04 (integrity, score 3/5/5), not confidentiality, so this isn't
   double-covered by another backlog item - it belongs to SR-03's own requirement text ("enforced
   consistently in UI and API workflows"). Fixed by adding a confidentiality check inside the display loop
   that skips a confidential appointment when the caller lacks the privilege, leaving
   `getAppointmentsInTimeSlot` itself untouched.

New tests, same red -> green pattern as the rest of this mitigation:
`ConfidentialAppointmentAccessControlTest` gained `getLastAppointment_filtersConfidentialAppointmentForUnauthorizedUser`
(api module, +1 test). A new `ConfidentialAppointmentDashboardRegressionTest` (omod module, +6 tests)
covers both DWR methods (no throw, no leak), both dashboard-controller actions (no mutation when
unauthorized, still works when authorized), and both `getPatientsInAppointmentBlock` directions (no
confidential content for unauthorized, still visible for authorized).

| Measure | Before this extension | After |
|---------|------------------------|-------|
| `getLastAppointment` confidentiality check | absent | present |
| DWR `NullPointerException` risk on a filtered confidential appointment | present (2 call sites) | fixed |
| Dashboard-controller write to a confidential appointment without the privilege | possible | blocked (silent no-op) |
| `getPatientsInAppointmentBlock` confidential content for an unauthorized user | displayed (patient identity, type name, reason) | filtered out |
| API tests (`mvn clean install`) | 172 | 173 |
| OMOD tests (`mvn clean install`) | 126 | 132 |

Reproducibility for each of the four items follows the same pattern as the rest of this mitigation: run
`ConfidentialAppointmentAccessControlTest` / `ConfidentialAppointmentDashboardRegressionTest`, then revert
the corresponding fix (remove the filter on `getLastAppointment`, remove a null check, or remove the
confidentiality check inside `getPatientsInAppointmentBlock`) and re-run to see the specific test fail.

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

**Result - VERIFIED.** `mvn -B test` and `mvn clean install` both pass. The confidential-access
regression suite passes (including `ConfidentialAppointmentAccessControlTest`,
`ConfidentialAppointmentDashboardRegressionTest`, and the reporting-evaluator tests
`PatientToAppointmentDataEvaluatorTest` / `PersonToAppointmentDataEvaluatorTest`), and the full reactor
validation also passes, including omod tests. So the confidential appointment is filtered for the
unauthorized user across every read path and there is **no regression**.

| Command | Scope | Result |
|---------|-------|--------|
| `mvn clean install` (initial fix) | Full reactor (`appointmentscheduling`, `api`, `omod`) | `BUILD SUCCESS`; API tests `172` passed; OMOD tests `126` passed |
| `mvn clean install` (after completing AC1 coverage) | Full reactor (`appointmentscheduling`, `api`, `omod`) | `BUILD SUCCESS`; API tests `173` passed; OMOD tests `132` passed |

Screenshot evidence for the re-test is stored in the evidence folder:

![SR-03 final API validation](../evidence/SR-03%20testresults3.png)

![SR-03 final reactor validation](../evidence/SR-03%20testresults4.png)

![SR-03 AC1-completion API tests](../evidence/SR-03%20AC1-completion%20API%20tests.png)

![SR-03 AC1-completion reactor](../evidence/SR-03%20AC1-completion%20reactor.png)

**Build prerequisites fixed to reach a green run** (both pre-existing, both unrelated to this finding, and
both unnoticed precisely because no CI stage compiles the module - the E-03 gap):

- **E-02:** a UTF-8 BOM in `AppointmentActivator.java` broke compilation - stripped.
- **E-03:** `getAppointmentsForPatientWithLogging()` called a non-existent method
  (`getAppointmentsForPatient`) - corrected to the existing `getScheduledAppointmentsForPatient`. (The
  method's PII-logging concern is left to the logging issue; only the compile break was fixed.)

> Validation note: current runs show both `mvn -B test` and `mvn clean install` pass successfully.

## (AI) tooling note

The mapping of entry points, the choke-point design, and the catch on the internal double-booking caller
were AI-assisted (Claude Code). The change deliberately **mirrors a pattern already present in the module**
(the evaluators' `hasPrivilege` check) rather than introducing a new mechanism, and was reviewed against
the module's existing behaviour to avoid regressions. I verified the actual fixture behaviour and the test
assertions manually rather than trusting them as generated, and kept the change limited to the
service/test/doc layers so the tool did not rewrite unrelated logic.

The first entry-point map was accepted as a narrative claim ("the DWR views all read through the filtered
methods") rather than checked against the code, and that narrative turned out to be incomplete. Re-deriving
it (AI-assisted) by grepping every `getAppointmentDAO()` call site and tracing each unfiltered method's
callers through `config.xml` and the JSPs found the four gaps closed under "Completing AC1" above. I
verified each one manually - reading the actual caller code, confirming the fixture data, and running the
test red before green - before accepting it. The lesson: "100% of entry points" has to be derived from the
code, not asserted from a caller map written before the fix.

## NEN-7510:2024-2 mapping

- 8.4 / 5.15 access control · 8.3 information access restriction
- 8.28 secure coding (authorization enforced at the service entry point)
- 8.29 security testing (the red -> green re-test)

## Evidence

| Item | Location |
|------|----------|
| Code change | `api/.../impl/AppointmentServiceImpl.java` |
| Re-test (red -> green) | `api/.../security/ConfidentialAppointmentAccessControlTest.java` |
| AC1-coverage extension code changes | `omod/.../web/DWRAppointmentService.java`, `omod/.../web/controller/PatientDashboardAppointmentExtController.java` |
| AC1-coverage extension re-test (red -> green) | `omod/.../security/ConfidentialAppointmentDashboardRegressionTest.java` |
| Initial-fix validation screenshots | `documentation/evidence/SR-03 testresults3.png`, `documentation/evidence/SR-03 testresults4.png` |
| AC1-coverage extension validation screenshots | `documentation/evidence/SR-03 AC1-completion API tests.png`, `documentation/evidence/SR-03 AC1-completion reactor.png` |
| Build-prerequisite fixes | E-02 (`AppointmentActivator.java` BOM), E-03 (`AppointmentServiceImpl` method typo) |
| Finding sources | `pentest-report.md` (#36), threat model (#51), `cia-analysis.md` §8 (#29) |

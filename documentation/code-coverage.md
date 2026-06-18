# Code Coverage

## Objective

The objective of this task was to configure and activate code coverage measurement for the Appointment Scheduling Module and integrate coverage reporting into the CI/CD pipeline.

## Baseline Situation

At the start of the project, no code coverage tooling was configured.

As a result:

* Test coverage could not be measured.
* No coverage reports were generated.
* No coverage artifacts were available through the CI pipeline.
* Coverage metrics could not be used to evaluate software quality.

The baseline coverage report generated after configuring JaCoCo is shown below.

![Base Coverage Report](evidence/BaseCoverage.png)

## Implemented Improvement

JaCoCo was configured through the Maven build process using the `jacoco-maven-plugin`.

The plugin generates coverage reports during test execution and produces:

* HTML coverage reports
* XML coverage reports
* Coverage execution data

A dedicated GitHub Actions workflow was added to the CI/CD pipeline. The workflow automatically:

1. Executes the automated test suite.
2. Generates JaCoCo coverage reports.
3. Uploads the reports as GitHub Actions artifacts.

This ensures that coverage information is available for every relevant build and can be reviewed independently of a local development environment.

## Coverage Results

Coverage was measured using the following command:

```bash
mvn -pl api clean test
```

Results:

| Metric       | Coverage |
| ------------ | -------- |
| Instructions | 70%      |
| Branches     | 63%      |
| Lines        | 72%      |
| Methods      | 76%      |
| Classes      | 88%      |

## Coverage Target

A minimum line coverage target of 70% was selected.

### Rationale

The Appointment Scheduling Module is a legacy OpenMRS component containing a significant amount of existing functionality and technical debt. A 70% threshold provides a realistic balance between development effort and software quality while ensuring that most business logic is exercised by automated tests.

The measured line coverage of 72% exceeds the defined quality target.

## Coverage Interpretation and Risk-Based Testing

The measured line coverage of 72% demonstrates that a substantial portion of the codebase is executed during automated testing. However, code coverage alone is not considered sufficient evidence of software quality or security.

Coverage metrics indicate which code is executed by tests, but they do not demonstrate whether critical functionality, security controls, or high-risk scenarios have been adequately validated.

For this reason, coverage results were evaluated together with the CIA analysis performed for the Appointment Scheduling Module.

### Coverage and CIA Alignment

The CIA analysis identified several high-priority risks requiring mitigation before release, including:

* Unauthorized access to appointment records.
* Exposure of confidential appointment types.
* Incorrect scheduling information affecting appointment integrity.
* Missing or altered audit information.
* Insufficient privilege enforcement in the UI and API.

The existing automated test suite covers a significant portion of the appointment scheduling functionality, contributing to the measured 72% line coverage. In addition, the project team reviewed whether the implemented tests exercise the areas associated with the highest CIA risks.

Coverage is therefore used as a supporting quality metric rather than as a standalone objective.

### Coverage Traceability Matrix

The table below maps every asset from the CIA risk register (`cia-analysis.md` §5/§7) to the class(es) that implement it, the **per-class line coverage** measured by JaCoCo (`mvn -pl api clean test`, `api/target/site/jacoco/jacoco.xml`), the automated test(s) that exercise it, and a verdict based on reading what those tests actually assert — not just whether the lines were executed.

| CIA Asset | Risk Score | Key Class(es) | Line Coverage | Test(s) | What Is Actually Verified | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| Appointment records | 20 (Unacceptable) | `AppointmentServiceImpl` | 90.4% (412/456) | `AppointmentServiceTest`, `ConfidentialAppointmentAccessControlTest` | CRUD and constraint-based retrieval are well covered. However, `ConfidentialAppointmentAccessControlTest` is a documented penetration-test case (issue #36) that **passes by proving a leak**: a user with only "View Appointments" can read a confidential appointment via `getAppointment()` / `getAllAppointments()`. The fix exists on a separate branch (`Mitigation/SR-03`) but is not yet merged into this branch. | Gap — high line coverage, but the dominant confidentiality risk is demonstrated-unmitigated, not tested-safe. |
| Confidential appointment types | 15 (Unacceptable) | `AppointmentType` (63.6%), `AppointmentTypeValidator` (94.6%) | see above | `AppointmentTypeServiceTest` (incl. `saveAppointmentType_shouldSaveConfidentialAppointmentType`), `PatientToAppointmentDataEvaluatorTest` / `PersonToAppointmentDataEvaluatorTest` (100%) | The `confidential` flag persists correctly, and the **reporting export path** correctly filters confidential appointments for an unprivileged user (`evaluate_shouldReturnPatientDataForNonConfidentialAppointments`). The **core service and REST paths do not** — same root cause as the row above (`PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS` is only referenced in the two reporting evaluators, never in `AppointmentService.java`). | Partially tested — enforcement is inconsistent between entry points, and only the safe path is proven. |
| Appointment requests and notes | 15 (Unacceptable) | `AppointmentRequest` (95%), `AppointmentRequestValidator` (90.9%) | see above | `AppointmentRequestServiceTest` | CRUD, validation, and void/unvoid lifecycle are covered. No test (and no production code) masks or restricts the free-text `reason` / `cancel_reason` fields for unauthorized users, or verifies they are excluded from logs (SR-02 is not implemented yet). | Gap — functional coverage only; the confidentiality control itself does not exist. |
| Appointment blocks and time slots | 15 (Unacceptable) | `AppointmentBlock` (84.8%), `AppointmentBlockValidator` (90.9%), `TimeSlot` (76%), `TimeSlotValidator` (90%) | see above | `AppointmentBlockValidatorComponentTest` (e.g. `shouldNotAllowCreationOfOverlappingAppointmentBlock`), `TimeSlotServiceTest` | Overlap / double-booking is explicitly created and rejected in a test. Void/unvoid and constraint-based retrieval are also covered. | Adequately tested. |
| Provider schedules | 8 (Acceptable w/ mitigation) | `ProviderSchedule` (92.3%), `ProviderScheduleValidator` (86.7%), `HibernateProviderScheduleDAO` (84.6%) | see above | `ProviderScheduleServiceTest` | CRUD and validation paths are covered. | Adequately tested. |
| Appointment status history | 8 (Acceptable w/ mitigation) | `AppointmentStatusHistory` (88.5%), `AppointmentStatusHistoryValidator` (85.7%) | see above | `AppointmentStatusHistoryServiceTest` | Save, retrieval, and status-change transitions are covered. No test asserts that historical records are immutable or protected from tampering after creation (SR-05 not implemented). | Partially tested — functional behaviour only, no tamper-resistance evidence. |
| Audit metadata (creator, changed-by, void reasons) | 8 (Acceptable w/ mitigation) | Inherited `Auditable` / `Voidable` fields on `Appointment`, `AppointmentType`, etc. | n/a (platform base class) | None dedicated | No module-specific test verifies these fields resist modification through a normal (non-admin) module operation. Coverage of the entity classes does not, by itself, exercise this guarantee. | Gap — relies entirely on unverified OpenMRS platform behaviour. |
| UI and privilege configuration | 8 (Acceptable w/ mitigation) | `AppointmentService.java` (41 `@Authorized` annotations) | 90.4% (via `AppointmentServiceImpl`) | `AppointmentServiceTest` | Tests exercise the *authorized* path for each privilege (e.g. `PRIV_VIEW_APPOINTMENTS`, `PRIV_MANAGE_APPOINTMENT_BLOCKS`). No test asserts that a user **lacking** a given privilege is denied — except the confidentiality privilege, where the existing test proves the opposite (see row 1). | Partially tested — "happy path" authorization only; negative/denied-access cases are missing. |

**Reading the matrix:** of the four "Unacceptable" (score ≥15) confidentiality/integrity risks, only the integrity risk (appointment blocks/time slots) is adequately tested. All three confidentiality risks share the same root cause — `PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS` is enforced in the reporting evaluators but not in the core service, REST, or web layers — and this is not a blind spot in the analysis: it is an open, reproducible finding (`pentest-report.md` F-01, issue #36) with a passing test that documents the leak rather than preventing it. This is the concrete illustration of why 72% line coverage cannot be read as "the module is adequately tested": the uncovered or shallow-covered 28%, and even some of the covered 72%, is concentrated in exactly the areas the CIA analysis flagged as highest-risk.

### Limitations of Coverage Metrics

A high coverage percentage does not guarantee that critical functionality is tested correctly.

For example, it is possible to achieve a relatively high coverage percentage while still missing tests for security-critical functionality such as authorization checks, confidential appointment handling, or audit logging behaviour.

Therefore, coverage results should always be interpreted together with:

* Risk assessments.
* Security analyses.
* Functional test results.
* Code reviews.

The goal is not to maximize coverage at all costs, but to ensure that testing efforts focus on the most important business and security risks.

## Evidence

Coverage reports are generated in:

```text
api/target/site/jacoco
```

The CI pipeline publishes these reports as downloadable GitHub Actions artifacts, allowing reviewers to inspect coverage results without requiring a local build environment.

## Conclusion

Code coverage measurement has been successfully configured and integrated into the CI/CD process.

The project currently achieves 72% line coverage, exceeding the defined minimum target of 70%.

More importantly, testing efforts are evaluated in relation to the highest risks identified in the CIA analysis through the Coverage Traceability Matrix above. That review shows that high line coverage does not by itself mean the highest risks are covered: three of the four "Unacceptable" risks (all confidentiality-related) are only partially tested or have a known, unmitigated gap (F-01, issue #36), while the fourth (schedule integrity) is adequately tested. This is a more useful signal than the aggregate 72% figure and should drive the next testing priorities: closing the confidentiality enforcement gap on the core/REST paths and adding denied-access and audit-immutability tests.

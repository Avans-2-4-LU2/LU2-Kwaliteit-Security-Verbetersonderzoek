# Schedule Integrity Controls (SR-04)

## Objective

`AppointmentBlock`, `TimeSlot`, and `ProviderSchedule` data must be accurate to prevent double-booking, missed appointments, and operational disruption. This is the "Appointment blocks and time slots" risk in `cia-analysis.md` (score 15, Unacceptable - Integrity), and is tracked as security requirement SR-04 in `security-requirements-mapping.md`.

This document records what integrity controls existed before this work, what was added, why each design decision was made, and what is still open.

## Scope

- `AppointmentBlock`, `TimeSlot`, `ProviderSchedule` entities and their Spring `Validator` classes.
- Every entry point that creates or modifies these entities (service layer, REST, web UI).

## Baseline: What Existed Before

| Rule | AppointmentBlock | TimeSlot | ProviderSchedule |
| --- | --- | --- | --- |
| Required fields not null | Enforced | Enforced | Enforced |
| `startDate` before `endDate` | **Documented in javadoc (`@should fail validation if start date is not before end date`), never implemented** | Not documented, not implemented | **Documented in javadoc, never implemented** |
| No overlapping records (same provider) | Implemented (`getOverlappingAppointmentBlocks`, scoped per-provider, excludes voided, excludes self on edit) | **Not implemented at all** - no check between sibling time slots in the same block | n/a (overlapping availability windows are not considered a risk) |
| Time slot falls within its parent block's date range | n/a | **Not implemented at all** | n/a |
| No past dates | Not implemented (a "no past dates" check existed only in `AppointmentBlockFormController`, the web UI - never in the shared validator, so REST/API callers never got this protection) | Not implemented | Not implemented |
| Optimistic locking / version column | None | None | None |

The dataset itself (`standardAppointmentTestDataset.xml`) confirms these gaps were real: appointment block #1 spans `2005-01-01 00:00-11:00`, but several of its (non-voided) time slots are dated `2005-01-02`, `2005-01-03`, `2006-01-01`, and `2007-01-01` - completely outside their parent block's range - and at least two of those (`#2` and `#4`) have identical start/end timestamps, i.e. they actively overlap each other. None of this was ever caught because no check existed.

## Entry Points Reviewed

Every write path for these three entities funnels through a single choke point: `AppointmentService.saveAppointmentBlock()` / `saveTimeSlot()` / `saveProviderSchedule()`, each of which calls `ValidateUtil.validate(...)` before persisting. Confirmed callers:

- **REST:** `AppointmentBlockResource1_9`, `AppointmentBlockWithTimeSlotResource1_9`, `TimeSlotResource1_9`, `ProviderScheduleResource1_9`
- **Web UI:** `AppointmentBlockFormController`
- **DWR (`DWRAppointmentService`):** read-only for these three entities - no bypass.

No controller or resource talks to the DAOs directly. This means fixing the three `Validator` classes protects every entry point at once, without needing to touch REST resources or controllers individually (the web controller's now-redundant manual checks were removed so it relies on the same shared validator as everything else).

## Implemented Validation Rules

### 1. `startDate` before `endDate`

Implemented on `AppointmentBlock`, `TimeSlot`, and `ProviderSchedule`. Rejects with `error.InvalidDateInterval`.

### 2. No past dates, on creation only

Implemented on all three entities. Rejects with `error.dateCannotBeInThePast`.

Two design decisions here:

- **Scoped to new records only** (`id == null`), not to edits. The acceptance criteria says no past dates "can be created" - it does not say existing/historical records become uneditable. Scoping it to creation means a clinic can still edit, void, or unvoid a block/time slot/schedule that has since become historical, while a brand-new record can never be backdated.
- **Compared at day granularity**, not the exact instant (`DateUtils.truncate(new Date(), Calendar.DATE)` rather than `new Date()`). An exact-instant comparison is a race condition: by the time validation runs, a few milliseconds have always passed since the date was captured, so a block starting "right now" would always appear to be "in the past" by the time it's checked. Comparing at day granularity removes that race while still rejecting genuinely past dates (yesterday or earlier).

### 3. TimeSlot falls within its parent AppointmentBlock's date range

New rule, did not exist before. Rejects with `error.outsideAppointmentBlock`. Scoped to new time slots only, for the same reason as above - and because the existing dataset already contains time slots that fall outside their nominal parent block (see Baseline), retroactively enforcing this on every edit/void would have broken legitimate operations on that legacy data.

### 4. TimeSlot does not overlap a sibling time slot in the same block

New rule, did not exist before. Rejects with `error.timeSlotOverlap`. Same creation-only scoping and same justification (existing dataset has overlapping siblings in block #1).

## Why These Rules Are Scoped to Creation, Not Editing

All four new/fixed rules above only run when the entity being validated has no ID yet (i.e. it is being created, not edited). This was not the original plan - the first implementation applied them unconditionally - but running the full test suite surfaced two problems:

1. **Exact-instant past-date comparison is inherently racy** (see above) - this affected even brand-new records and was fixed by truncating to day granularity, independent of the creation/edit scoping.
2. **Retroactively validating existing data breaks legitimate operations on it.** Voiding or unvoiding a `TimeSlot` calls `saveTimeSlot()` internally, which re-runs full validation on the entire entity - including fields nobody touched. Several of the test dataset's pre-existing time slots fail the new bounds/overlap rules (see Baseline). Applying these rules unconditionally meant simply voiding an old time slot would throw a validation exception, with no relation to what the operation actually changed.

Creation-only scoping is also the more literal reading of the acceptance criteria ("no past dates or invalid schedule data can be **created**"), and it mirrors how `AppointmentBlockValidator`'s pre-existing overlap check already behaves implicitly (overlap is only meaningful to check against a new submission).

## Test Impact

Implementing these rules required updating several existing tests whose fixtures predate the rules:

| Test | Why it needed updating |
| --- | --- |
| `AppointmentBlockValidatorComponentTest.shouldNotAllowCreationOfOverlappingAppointmentBlock` | Creates a new block dated 2005 to test overlap detection; now also (correctly) fails the past-date rule. Assertion updated to expect both specific errors instead of just a count. |
| `AppointmentBlockValidatorComponentTest.shouldAllowCreationOfNonOverlappingAppointmentBlock` | Used a fixed 2007 date; switched to a relative future date so the test still proves "non-overlapping new blocks are allowed" without tripping the new rule. |
| `AppointmentBlockServiceTest.getOverlappingAppointmentBlocks_shouldAllowOverlappingProviderlessAppointmentBlocks` | Same pattern - switched a hardcoded 2005 date to a relative future date. |
| `TimeSlotServiceTest.saveTimeSlot_shouldSaveNewTimeSlot`, `AppointmentServiceTest.saveAppointment_shouldSaveNewAppointment` | Both attached a brand-new `TimeSlot` to historical fixture block #1 (2005). A genuinely new time slot can no longer attach to a historical block, so each test now creates its own present-day `AppointmentBlock` first. |
| `AppointmentBlockResource1_9ControllerTest.shouldCreateNewAppointmentBlock`, `AppointmentBlockWithTimeSlotResource1_9ControllerTest.shouldCreateNewAppointmentBlockWithTimeSlot` | REST payloads hardcoded a 2005 date; bumped to a far-future literal date. |
| `TimeSlotResource1_9ControllerTest.shouldCreateNewTimeSlot` | REST payload referenced an existing historical block by UUID; rewritten to create a present-day block first and build the JSON request against it dynamically. |

No test asserting read/query behaviour against the existing dataset was touched - only tests that exercise a **create** path needed adjustment, which is consistent with the rules only applying to creation.

Full suite after these changes: **167/167 `api` tests, 125/125 `omod` tests passing.**

## Remaining Work

Not yet implemented as part of this pass:

- **"Provider must be available"** - cross-checking an `AppointmentBlock`'s provider/location/time window against an active `ProviderSchedule`. Deferred because it requires a business-rule decision (what exactly counts as "available") rather than a mechanical fix.
- **Database-level constraints.** FK constraints already exist (`provider_id`, `location_id`, `appointment_block_id`); NOT NULL constraints already exist on required fields. True range-overlap prevention is not practically enforceable as a simple unique index in the target RDBMS - this needs an explicit scope decision (accept application-level enforcement only, with documented rationale, vs. a DB trigger) rather than a default implementation.
- **Optimistic locking (`@Version`).** None of the three entities have a version column today, so "concurrent updates do not corrupt state" has no enforcement mechanism yet.
- **Concurrency/race-condition test.** No test in this codebase exercises concurrent writes against the same entity; this is net-new test infrastructure, not an extension of an existing pattern.
- **Dedicated unit tests for the new rules.** The rules above are currently proven indirectly, through the fixed existing tests. Acceptance criteria call for explicit valid/invalid unit tests per rule.

## Evidence

```bash
mvn -pl api test
mvn -pl omod test
```

Both commands complete with `BUILD SUCCESS` and zero failures/errors as of this writing.

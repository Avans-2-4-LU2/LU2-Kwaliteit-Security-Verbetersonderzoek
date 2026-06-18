# Schedule Integrity Controls (SR-04)

## Objective

`AppointmentBlock`, `TimeSlot`, and `ProviderSchedule` data must be accurate to prevent double-booking, missed appointments, and operational disruption. This is the "Appointment blocks and time slots" risk in `cia-analysis.md` (score 15, Unacceptable - Integrity), and is tracked as security requirement SR-04 in `security-requirements-mapping.md`.

This document records what integrity controls existed before this work, what was added, and why each design decision was made.

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
| Provider must be available (cross-check against `ProviderSchedule`) | **Not implemented at all** | n/a | n/a |
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

### 5. Provider must be available, fail-open

New rule, did not exist before. For a new `AppointmentBlock` with a provider and location set, checks whether at least one of the provider's non-voided `ProviderSchedule` records (matched by location) covers both the block's date range and its time-of-day window. Rejects with `error.providerNotAvailable` - but **only if the provider has at least one `ProviderSchedule` defined at all**. If the provider has none, the check is skipped entirely and the booking is allowed.

This is deliberately fail-open rather than fail-closed, for two reasons:

- **Risk-register priority.** `cia-analysis.md` treats "Appointment blocks and time slots" (score 15, Unacceptable) and "Provider schedules" (score 8, Acceptable with mitigation) as two separate risks. The required mitigation for the score-15 risk is "data validation, concurrency control, and backup" - it does not mention provider availability. The availability cross-check is the named (optional) mitigation for the *separate*, lower-priority "Provider schedules" risk ("implement integrity checks and availability safeguards"). Fail-open matches that risk's "optional/recommended" rigor rather than over-enforcing a control at a stricter level than the risk register calls for.
- **Practical blast radius.** Fail-closed would require every provider used in a test (or in production) to have a fully-configured `ProviderSchedule` before any block could ever be created for them. In the existing dataset, most provider/location combinations have no schedule at all; enforcing fail-closed would have broken multiple already-fixed tests and effectively made the feature unusable for any provider who hasn't set up a schedule yet.

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

## Database-Level Constraints: Decision

The acceptance criteria call for "database constraints (unique indexes, foreign keys)" in addition to application-level validation. The actual decision:

- **FK constraints**: already present (`provider_id`, `location_id`, `appointment_block_id`, `creator`/`changed_by`/`voided_by`) - no change needed.
- **NOT NULL constraints**: already present on required fields - no change needed.
- **Range-overlap prevention as a unique index**: explicitly **not implemented**. A simple unique index cannot express "no two rows with overlapping date ranges for the same provider" - that needs either a DB-specific exclusion constraint (e.g. PostgreSQL `EXCLUDE USING gist`) or a trigger, neither of which is practical to maintain across the RDBMS targets this module already supports. Overlap is therefore enforced at the application level only (`AppointmentBlockValidator`/`TimeSlotValidator`), which is an accepted, documented limitation rather than an oversight.
- **Optimistic locking (`version` column)**: **implemented** as the one concrete new DB-level constraint, on `AppointmentBlock` and `TimeSlot` only (not `ProviderSchedule` - see the provider-availability section above for why that entity is out of scope for this risk). Added via Liquibase changesets `20260618-1-appointmentscheduling-add-version-columns` / `...-2-...` and Hibernate `<version>` mappings in `AppointmentBlock.hbm.xml` / `TimeSlot.hbm.xml`. This directly satisfies "concurrent updates do not corrupt state": Hibernate includes `WHERE version = ?` on every `UPDATE`, so a write based on stale data affects zero rows and throws `StaleObjectStateException`, rather than silently overwriting a concurrent change.

All existing test dataset fixtures (`appointmentTestDataset.xml`, `standardAppointmentTestDataset.xml`, `standardWebAppointmentTestDataset.xml`) needed a `version="0"` attribute added to every `appointment_block`/`time_slot` row - the test framework's XML dataset loader inserts an explicit `NULL` for any column not listed in a row (rather than omitting it and letting the column's SQL `DEFAULT` apply), which the new `NOT NULL` constraint rejected.

## Dedicated Unit Tests for the New Rules

Each rule above is now also covered by explicit valid/invalid unit tests calling the `Validator` directly (not just proven indirectly through the fixed existing tests):

- `AppointmentBlockValidatorComponentTest` - added: ordering, isolated past-date rejection, edit-exemption from the past-date rule, provider-availability rejection (schedule exists but block falls outside it), provider-availability pass (block falls within the schedule), and fail-open pass (provider has no schedule at all).
- `TimeSlotValidatorComponentTest` (new file) - ordering, isolated past-date rejection, outside-block rejection, sibling-overlap rejection, a fully valid new time slot, and edit-exemption using the dataset's known-messy time slot #1 (outside its block's range and overlapping a sibling) to prove editing legacy data still works.
- `ProviderScheduleValidatorComponentTest` (new file) - ordering, isolated past-date rejection, a fully valid new schedule, and edit-exemption.

Each "isolated" test was constructed so only one rule could plausibly fire (e.g. picking dates that are individually within a block's bounds but reversed relative to each other, to test ordering without also tripping the bounds check) - this is called out in code comments per test rather than relying on the reader to infer it.

Full suite: **183/183 `api` tests** (167 existing + 16 new), **125/125 `omod` tests**.

## Concurrency Test

`ScheduleConcurrencyTest` (new file, `api/src/test/java/.../api/`) proves the `version` column added above actually rejects a concurrent modification rather than silently allowing one writer to overwrite another's change, for both `AppointmentBlock` and `TimeSlot`.

**Design choice: no real second thread or second database connection is used**, and this is deliberate, not a shortcut. Every test method in this codebase runs inside a transaction that Spring rolls back afterwards, so other tests never see leftover data. A genuinely independent Hibernate session (opened on its own connection) would not see this test's not-yet-committed data at all - proving the mechanism that way would require suspending that rollback wrapper for the test method and then manually cleaning up every row it touched afterwards, to avoid leaking committed data into the shared test database that every other test class in the run reuses. That's extra risk (test pollution across the whole suite) for no extra confidence, because:

Hibernate's version check is purely mechanical: on every `UPDATE` of a versioned entity it issues `UPDATE ... WHERE id = ? AND version = ?` and treats zero affected rows as a conflict, regardless of *how* the row's version came to no longer match - a real second thread, a second process, or (as done here) a raw SQL statement that bumps the `version` column directly, bypassing Hibernate's object model entirely. From Hibernate's perspective those are indistinguishable. So each test:

1. Saves a fresh block/time slot the normal way (`version` = 0).
2. Reloads it (simulating what a second request would see).
3. Runs a raw SQL `UPDATE ... SET version = version + 1` directly against the row - simulating a concurrent writer having already committed a change - via `AdministrationService.executeSQL(...)`, entirely within the same (eventually rolled back) test transaction.
4. Modifies the reloaded, now-stale in-memory copy and saves it again, and asserts that `org.hibernate.StaleObjectStateException` is thrown rather than the write silently succeeding.

Both tests pass, confirming the version columns added above are wired up correctly end to end - not just present in the schema, but actually enforced by Hibernate on every save.

Full suite: **185/185 `api` tests**, **125/125 `omod` tests**.

## Evidence

```bash
mvn -pl api test
mvn -pl omod test
```

Both commands complete with `BUILD SUCCESS` and zero failures/errors as of this writing (185/185 `api`, 125/125 `omod`).

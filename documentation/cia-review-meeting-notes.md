# CIA Review Meeting Notes

## Meeting Details
- Date: 2026-06-15
- Location/Platform: Discord Call
- Attendees:
  * Sander Bosselaar
  * Mathijs Haest
  * Reinout van der Geest
  * Thomas Dubbeld

## Agenda
1. Review CIA analysis document
2. Validate likelihood scores
3. Agree on treatment strategies
4. Sign off on risk appetite and thresholds

## Decisions

| Risk                                         | Proposed Treatment     | Final Treatment         | Decided by    | Reason                                                                                                                                                      |
| -------------------------------------------- | ---------------------- | ----------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Appointment records (score 20)               | Mitigate (required)    | Mitigate before release | Project Group | Contains patient-linked scheduling data, so unauthorized access would directly expose health-related information and exceeds the confidentiality threshold. |
| Appointment requests and notes (score 15)    | Mitigate (required)    | Mitigate before release | Project Group | Free-text notes can include clinical context or private circumstances, so disclosure is not acceptable.                                                     |
| Confidential appointment types (score 15)    | Mitigate (required)    | Mitigate before release | Project Group | The confidential flag controls restricted visibility, so unauthorized disclosure would reveal appointment categories that must stay hidden.                 |
| Appointment blocks and time slots (score 15) | Mitigate (required)    | Mitigate before release | Project Group | Incorrect scheduling data can cause double-booking or missed appointments, which makes integrity protection necessary.                                      |
| Provider schedules (score 8)                 | Mitigate (optional)    | Mitigate                | Project Group | Wrong or unavailable schedules can delay care and disrupt booking continuity, so integrity controls are needed.                                             |
| Appointment status history (score 8)         | Mitigate (recommended) | Mitigate                | Project Group | Status history is needed for traceability and patient journey reconstruction, so it must remain complete and available.                                     |
| Audit metadata (score 8)                     | Mitigate (recommended) | Mitigate                | Project Group | Creator, changer, and void metadata support accountability and incident review, so tampering would weaken investigations.                                   |
| UI and privilege configuration (score 8)     | Accept or Mitigate     | Mitigate                | Project Group | Privilege checks enforce confidentiality and integrity in the UI and API, so the control must be validated end to end.                                      |


### Additional Decisions

- The project group approved the defined risk appetite and category-specific risk thresholds as documented in the CIA analysis.
- Risks are evaluated against the threshold of their applicable category rather than a single global threshold.
- Risks affecting patient confidentiality, patient safety, or schedule integrity are subject to stricter acceptance criteria than reputation-only risks.
- Risks classified as "Unacceptable" within their category must be mitigated before release.

## Action Points

The action points are grouped by control area so the follow-up remains readable while still covering all eight CIA risks from the analysis.

| Action | Owner | Due date |
| --- | --- | --- |
| Confirm access control and privilege checks for appointment records, requests, confidential types, and UI/API entry points | Project Group | Before next sprint review |
| Add or update tests for denied access and confidential visibility to prove the controls work as intended | Project Group | Before next sprint review |
| Validate integrity controls for appointment blocks, time slots, and provider schedules to prevent incorrect booking data | Project Group | Before next sprint review |
| Review audit logging and traceability for appointment status history, creator/changed-by metadata, and void reasons | Project Group | Before next sprint review |
| Perform an end-to-end review of privilege enforcement across the UI and backend so no entry point bypasses the controls | Project Group | Before next sprint review |

## Next Review
- Date: Sprint Review Meeting
- Trigger: new sprint, major feature change, or security incident

## Sign-off
- Approved by: Project Group (S. Bosselaar, M. Haest, R. van der Geest, T. Dubbeld)
- Date: 2026-06-15


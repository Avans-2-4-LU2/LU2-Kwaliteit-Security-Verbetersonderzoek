# Risk Matrix - OpenMRS Appointment Scheduling Module

## Purpose

This document takes the identified security risks for the appointment scheduling module and ranks them by how serious they really are, so the team can decide what to deal with first. It follows the WS03 / ISO 27005 method: each risk is scored as **Kans x Impact** (likelihood x impact) on a 5x5 grid.

The risks come mainly from the SBOM/SCA findings in `sbom-analysis.md` (#17). The structure leaves room to add threat-model risks (#30) and CI/CD risks (#33) later.

**NEN-7510 controls:** NEN-7510-1 6.1.2 (a risk analysis is the basis of the ISMS), plus a per-risk mapping to NEN-7510-2 controls (section 7).

---

## 1. Risk Criteria

Before scoring any risk, we define the measuring stick. Without this you cannot justify *why* a risk is red or green.

### 1.1 How a risk is scored

```
Risk score = Kans (1-5) x Impact (1-5)   ->   range 1-25
```

- **Kans (likelihood)** - how probable is it that the risk actually happens?
- **Impact** - how bad are the consequences if it does?

Both are rated on a 1-5 scale; the detailed definitions of each level are in section 2.

### 1.2 Risk appetite (which scores are acceptable)

The score puts each risk in one of three bands, each with a required action (WS03 slide 22):

| Score | Level | Colour | Required action |
|-------|-------|--------|-----------------|
| >= 15 | Unacceptable | Red | Address immediately |
| 8 - 14 | Elevated | Orange | Mitigation plan with a deadline |
| <= 7 | Acceptable | Green | Monitor and re-assess periodically |

### 1.3 Category threshold (patient safety weighs heavier)

WS03 slide 31 states the bar is **stricter for patient safety than for reputation**. So any risk whose impact directly touches **patient data or patient safety** is treated as at least **Elevated (orange)**, even if its raw score would land in the green band. This reflects that medical data is the strictest category under the AVG (GDPR art. 9).

### 1.4 Approval (who may accept a risk)

Accepting a risk is never "ignore and forget". Any accepted residual risk must be documented (who, when, why) and approved at the right level.

In a real organisation these levels would map to different management roles. This is a 4-person student project with no separate management layer, so the **project team collectively holds the risk-owner role**. To keep the four-eyes principle (the same one we already apply with required PR reviews), an acceptance must be agreed by the team and recorded here, with at least one team member other than the author confirming it.

| Level | Who may accept it |
|-------|-------------------|
| Red (>= 15) | Not acceptable - must be reduced, not accepted |
| Orange (8 - 14) | Team decision, confirmed by a second team member, with a documented review date |
| Green (<= 7) | Team decision, recorded in this document |

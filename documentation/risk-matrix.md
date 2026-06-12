# "status: v1, pending #30" 



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

---

## 2. Rating Scales

Section 1 set the formula (Kans x Impact). This section says what each number from 1 to 5 actually means, so a rating is a justified choice and not a guess - and so every team member rates the same way.

### 2.1 Kans (likelihood) scale

WS03 splits likelihood into two questions:

- **Blootstelling (exposure)** - is the weak spot actually present and reachable in our deployment?
- **Waarschijnlijkheid (probability)** - how likely is an attacker to use it?

We combine both into one 1-5 score, with **exposure as the main driver**: a weak spot that is hard to reach stays low even if the vulnerability is popular to exploit. For the dependency risks from #17 we read exposure from the `bereikbaarheid` (reachability) factor and probability from the EPSS score and CISA KEV status.

| Kans | Label | Meaning | Typical case in our project |
|------|-------|---------|------------------------------|
| 1 | Very unlikely | The exploit path is effectively blocked | Blocked by our setup (e.g. needs JDK 9+, we run JDK 8) |
| 2 | Unlikely | Reachable only with non-default config or admin rights, and little exploitation activity | Needs a special configuration or high privilege; low EPSS |
| 3 | Possible | Present behind the hospital intranet (not internet-facing); exploitation is plausible | Most provided-scope dependency findings: reachable, but not exposed to the open internet |
| 4 | Likely | Easily reachable AND a known exploit or high exploitation probability | Internet-facing or bundled component with a public exploit / high EPSS |
| 5 | Very likely | Directly reachable AND confirmed exploited in the wild | On the CISA KEV list and reachable |

### 2.2 Impact scale

Impact is how bad the consequences are if the risk happens. The main driver in our context is what happens to **patient appointment data** (confidentiality and integrity) and to the **availability** of the scheduling service - the CIA triad. We also weigh legal (AVG / IGJ), financial and reputational damage.

| Impact | Label | Meaning in our context |
|--------|-------|------------------------|
| 1 | Negligible | No patient data and no service affected; cosmetic or internal only |
| 2 | Minor | Limited disclosure of non-patient data; minor, easily recovered disruption |
| 3 | Serious | Some patient appointment data exposed or altered, or a temporary outage of the scheduling service; recoverable |
| 4 | Very serious | Large-scale exposure or manipulation of patient appointment data; an AVG-reportable breach; significant outage |
| 5 | Catastrophic | Full system takeover (e.g. remote code execution); mass patient-data breach affecting patient safety; major legal and reputational fallout |

For the #17 findings we read impact from the CVSS confidentiality / integrity / availability metrics and whether the vulnerability can reach patient data.

---

## 3. Identified Risks and Rating

The risks below are grouped from the active Critical findings in `sbom-analysis.md` (#17). We group by **attack outcome** rather than listing each CVE separately, because the matrix is about risk scenarios, not individual packages (the same style as WS03). All ten active Critical findings are covered by three scenarios. High and Medium findings, and non-dependency risks from #30 (threat model) and #33 (CI/CD), can be added as extra rows later.

### 3.1 Risk register

| ID | Risk scenario | Source findings (#17) | Kans | Impact | Score | Level |
|----|---------------|-----------------------|------|--------|-------|-------|
| R-01 | Remote code execution via unsafe deserialization in platform libraries | C-02, C-03, C-04, C-08, C-10 | 3 | 5 | 15 | Red |
| R-02 | Data disclosure / DoS via XML external entity (XXE) processing | C-09, C-11, C-12, C-14 | 2 | 4 | 8 | Orange |
| R-03 | Arbitrary file write / RCE via module-upload path traversal | C-13 | 2 | 5 | 10 | Orange |

### 3.2 Justification per rating

**R-01 - RCE via deserialization (Kans 3 x Impact 5 = 15, Red)**
- *Kans 3 (Possible):* these are provided-scope libraries on the hospital intranet, not internet-facing, so exposure is limited. Probability is high - commons-collections and spring-web have high EPSS scores and well-known public exploits. Exposure caps the rating at "possible"; if this module exposed a deserialization endpoint to the open network it would rise to "likely".
- *Impact 5 (Catastrophic):* a successful exploit gives remote code execution - full takeover of the host, which can reach all patient data.

**R-02 - XXE (Kans 2 x Impact 4 = 8, Orange)**
- *Kans 2 (Unlikely):* exploiting these requires the application to parse attacker-controlled XML (e.g. a crafted Quartz job definition or Liquibase changelog), which usually needs write access or a specific entry point. EPSS scores are low to moderate.
- *Impact 4 (Very serious):* XXE can read local files and reach internal services, which could disclose configuration or patient data - an AVG-reportable breach.

**R-03 - Path traversal in module upload (Kans 2 x Impact 5 = 10, Orange)**
- *Kans 2 (Unlikely):* the OpenMRS module-upload endpoint requires an authenticated administrator (the CVSS vector shows high privileges required), and the EPSS score is very low.
- *Impact 5 (Catastrophic):* writing arbitrary files to the server can lead to remote code execution and full system compromise.

All three risks touch patient data, so the patient-safety category rule (section 1.3) keeps each at orange or above - which they already are.

# Requirements LU2 — openmrs-module-appointmentscheduling

Dit document legt de eisen (requirements) vast voor beide opdrachtonderdelen van LU2:
**onderhoudbaarheid** (deel 1) en **security & compliance** (deel 2). De eisen zijn
meetbaar geformuleerd, gekoppeld aan tooling en gemapt op de vier sprints, zodat elke
eis traceerbaar is naar een sprint, een deliverable en een NEN-7510-control.

## Uitgangspunten

- **Clean as you code.** De module is legacy. Strenge drempels gelden op *new/changed
  code* (de regels die je in deze sprints aanraakt). De volledige codebase meten we als
  **nulmeting (0-meting)**. Doel: alle eisen gehaald op nieuwe code én geen regressie op
  de baseline.
- **Controlnummering indicatief.** NEN-7510-2:2024 is gebaseerd op de ISO 27002:2022-
  structuur; de controlnummers hieronder verwijzen daarnaar. De norm is betaald —
  **verifieer nummering en formulering tegen jullie norm-/cursusexemplaar** voor je ze in
  het auditrapport opneemt.
- **OWASP ASVS** als toetsbare baseline voor de application security requirements
  (sluit aan op control 8.26).

---

## Deel 1 — Onderhoudbaarheid (ISO 25010)

Gemeten via SonarCloud (statische analyse) + JaCoCo (coverage). De quality gate dwingt
deze eisen af; bij overschrijding faalt de CI en wordt de PR-merge geblokkeerd.

| ID | ISO 25010-subkenmerk | Eis (op new code) | Gemeten via |
|----|----------------------|-------------------|-------------|
| NFR-M-01 | Analyseerbaarheid | Cognitive Complexity ≤ 15 per methode; geen nieuwe smells severity *blocker*/*critical* | SonarCloud |
| NFR-M-02 | Wijzigbaarheid | Maintainability Rating = A; Technical Debt Ratio ≤ 5% | SonarCloud (SQALE) |
| NFR-M-03 | Modulariteit / herbruikbaarheid | Duplicated Lines ≤ 3% | SonarCloud |
| NFR-M-04 | Testbaarheid | Cyclomatic Complexity ≤ 10 per methode; line coverage ≥ 80% | SonarCloud + JaCoCo |
| NFR-M-05 | Analyseerbaarheid | Geen dode/uitgecommentarieerde code; consistente, betekenisvolle naamgeving | SonarCloud + handmatige review |
| NFR-M-06 | Afdwingbaarheid | Quality Gate "Failed" ⇒ CI faalt en PR-merge wordt geblokkeerd | SonarCloud + branch protection |

### Nulmeting (0-meting) — vast te leggen na eerste scan

Leg vóór de eerste verbetering deze baseline van de **hele module** vast. Doel is *geen
regressie* op deze waarden gedurende het project.

| Metriek | Waarde bij start |
|---------|------------------|
| Lines of Code | `[invullen na 1e scan]` |
| Maintainability Rating (overall) | `[invullen]` |
| Technical Debt Ratio (overall) | `[invullen]` |
| # Code Smells (blocker / critical / major) | `[invullen]` |
| Coverage (overall) | `[invullen]` |
| Duplicated Lines % (overall) | `[invullen]` |

### Quality gate-inrichting

- Custom quality gate met "conditions on new code" voor NFR-M-01 t/m M-04.
- Gate koppelen aan branch protection: merge naar `main`/`develop` alleen bij "Passed".
- Per drempel documenteren waaróm die waarde gekozen is (onderbouwing scoort).

---

## Deel 2 — Security & Compliance (NEN-7510-2:2024)

### 2.1 Risicobasis (Sprint 2)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-01 | BIV/CIA-analyse: kroonjuwelen geïdentificeerd mét referenties; verwerkte gevoelige (patiënt)gegevens benoemd | Risicomanagement (NEN-7510-1 / ISO 27001 clausule 6.1) |
| NFR-S-02 | Risicocriteria vastgelegd: scoreschaal (kans × impact), risicobereidheid en grenswaarden | idem |

### 2.2 Threat modelling, risico's & attack surface (Sprint 2, bijgewerkt Sprint 3)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-03 | Threat model met C4-diagrammen (context-, container- én componentniveau); level 0 + level 1; threats geïdentificeerd (bv. STRIDE) | 8.27 secure architecture, 8.25 secure SDLC |
| NFR-S-04 | Risicomatrix met geïdentificeerde risico's, gescoord volgens NFR-S-02 | 8.27 |
| NFR-S-05 | Bow-tie voor de hoogste risico's, met preventieve én correctieve maatregelen | 8.27 |
| NFR-S-06 | Attack surface mapping: alle ingangen tot de module gedocumenteerd, impliciete trust benoemd, high-risk-ingangen gemarkeerd; threat model bijgewerkt o.b.v. nieuwe inzichten | 8.26 application security requirements |

### 2.3 Secure SDLC & projectorganisatie (Sprint 1)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-07 | Gescheiden test- en productieomgeving (GitHub Environments) met gescheiden configuratie én gescheiden secrets | 8.31 scheiding O-T-A-P, 8.9 configuratiebeheer, 8.24 cryptografie/secrets |
| NFR-S-08 | Protection rules + approval-gates op deployment naar productie | 8.31, 8.2/8.3 toegang |
| NFR-S-09 | README beschrijft: omgevingsinrichting, hoe testdata-in-productie wordt voorkomen, en onboarding van een nieuwe ontwikkelaar | 8.25 secure SDLC |
| NFR-S-10 | Gap-analyse van 3 gekozen NEN-7510-2 controls: huidige implementatie (wiki/GitHub/broncode) onderzocht + benodigde acties om compliant te zijn | 3 zelfgekozen controls |

### 2.4 Secure CI/CD-pipeline (Sprint 2 + 3)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-11 | SAST in de pipeline; build faalt boven gedefinieerde drempel | 8.28 secure coding, 8.29 security testing |
| NFR-S-12 | SCA op dependencies; CVE's leiden tot update-advies geprioriteerd op CVSS-score | 8.8 technische kwetsbaarheden |
| NFR-S-13 | SBOM per build (bv. CycloneDX JSON) — t.b.v. NEN-7510 én CRA | 8.8 |
| NFR-S-14 | Beleid voor omgang met false positives vastgelegd | 8.8 |
| NFR-S-15 | Risico-evaluatie van het CI-CD-proces zelf: risicomatrix + bow-tie voor het kritiekste risico | 8.25, 8.27 |

### 2.5 Logging & monitoring (Sprint 3)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-16 | Logging gap-analyse tegen NEN-7510 **8.15**: overzicht `Event | Gelogd? | Gevoelige data | Compliant 8.15`; nadruk op niet-gelogde events; gat tussen huidig en gewenst gedocumenteerd | 8.15 logging |
| NFR-S-17 | Logging aangevuld en compliant aan 8.15, met aandacht voor géén gevoelige data in logs | 8.15 |
| NFR-S-18 | Loggingtests: succesvolle én mislukte acties, en (voor zover mogelijk) afwezigheid van gevoelige gegevens; alle tests slagen | 8.15, 8.29 |

### 2.6 Testdekking (Sprint 3 — overlapt Deel 1)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-19 | Code coverage geactiveerd; gekozen coverage-% onderbouwd (contextafhankelijk); coveragerapport als CI-artefact (bv. GitHub Action) | 8.29 |

### 2.7 Penetration testing & mitigatie (Sprint 2 + 3)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-20 | Pentestplan gericht op de hoogste risico's; uitvoering met navolgbaar vastgelegde bevindingen (reproductiestappen) | 8.29 |
| NFR-S-21 | Per bevinding een onderbouwd besluit (oplossen / niet oplossen) → opgenomen in security backlog | 8.29, 8.8 |
| NFR-S-22 | Mitigaties doorgevoerd; ná bijgewerkt threat model herhaalde pentest die aantoonbaar verlaagd risico laat zien; gebruik van (AI-)tooling verantwoord | 8.29, 8.8 |

### 2.8 Security backlog (Sprint 2)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-23 | Geprioriteerde security requirements o.b.v. de gevonden risico's, beheerd als backlog | 8.26 |

### 2.9 Rapportage & traceability (Sprint 2 + 4)

| ID | Eis | Control |
|----|-----|---------|
| NFR-S-24 | Risk Assessment Report o.b.v. scanresultaten + security backlog; verwerkte gevoelige gegevens mét referenties; per vulnerability een mitigatie gekoppeld aan een NEN-7510:2024-2 maatregel; kostenraming (resources, tijd, budget) | diverse |
| NFR-S-25 | Traceability matrix: ≥ 3 NEN-7510:2024 controls, waarbij elk bewijs een traceerbaar artefact is | clausule 9 (interne audit) |
| NFR-S-26 | Auditrapport met secties: Executive Summary, Scope & Context, Audit Methodologie, Risico-analyse (≥ 4 bevindingen), SBOM & Supply Chain Security, Conclusie & Advies + bijlagen (traceability matrix, SBOM, SAST-output, risicomatrix, bow-tie/threat models, CRA-mapping) | clausule 9 |
| NFR-S-27 | CRA-mapping: SBOM + kwetsbaarhedenbeheer gekoppeld aan de relevante CRA-eisen | EU Cyber Resilience Act |
| NFR-S-28 | Niet-uitgevoerde items expliciet vastgelegd met onderbouwing | — |

---

## Sprint-mapping

| Sprint | Hoofdactiviteiten | Requirements | Belangrijkste deliverables |
|--------|-------------------|--------------|----------------------------|
| **1** (wk 5/6) | Gap-analyse 3 controls; projectorganisatie + CI-CD basis inrichten | NFR-S-07 t/m S-10 | Gap-analyse 3 controls, GitHub Environments (test+prod) met protection rules, README |
| **2** (wk 6/7) | BIV/CIA, threat modelling, risico's, scanners, pentest, RAR | NFR-S-01 t/m S-05, S-11 t/m S-15, S-20, S-21, S-23, S-24 | Threat model (C4), risicomatrix, bow-ties, SAST/SCA/SBOM in CI, security backlog, Risk Assessment Report |
| **3** (wk 7) | Attack surface, logging-gap & implementatie, coverage, re-test | NFR-S-06, S-16 t/m S-19, S-22 | Bijgewerkt threat model, attack surface-overzicht, logging-gap + implementatie + tests, coveragerapport |
| **4** (wk 8) | Afronden, aantoonbaar maken, rapporteren | NFR-S-25 t/m S-28 + restpunten | Traceability matrix, volledig auditrapport met bijlagen, CRA-mapping |

---

## Tooling-suggesties

| Doel | Tool(s) |
|------|---------|
| SAST | SonarCloud (security rules), CodeQL |
| SCA / dependencies | OWASP Dependency-Check, Snyk of GitHub Dependabot |
| SBOM | Syft / CycloneDX-plugin |
| Container/dependency-CVE | Trivy |
| Secret scanning | gitleaks of GitHub secret scanning |
| DAST / pentest | OWASP ZAP |
| Threat modelling | OWASP Threat Dragon |
| C4-diagrammen | Structurizr of PlantUML (C4-PlantUML) |
| Coverage | JaCoCo (rapport als CI-artefact) |
| Requirements-baseline | OWASP ASVS |

---

## Aandachtspunten

- **Selecteer expliciet de 3 NEN-7510-controls** voor de gap-analyse en motiveer de keuze;
  laat ze logisch aansluiten op de rest (bv. 8.15 logging, 8.8 kwetsbaarheden, 8.31 OTAP).
- **Herleidbaarheid** is de rode draad: elke bevinding moet koppelbaar zijn aan een control
  én aan de inrichting van het platform (pipeline-stap, gate-conditie, PR-review, artefact).
- **Gevoelige gegevens** consequent benoemen met referenties — dit komt terug in NFR-S-01,
  S-16/17 (logging) en S-24 (RAR).
- Vul de **nulmeting** in zodra de eerste SonarCloud-scan gedraaid is; daarna kun je de
  baseline-drempels concreet maken.
# Requirements - LU2 (openmrs-module-appointmentscheduling)

Requirements for both assignment parts: **maintainability** (Part 1) and
**security & compliance** (Part 2). Each requirement is measurable, tied to tooling,
and linked to a sprint and a NEN-7510 control.

**Principles**

- *Clean as you code* - strict thresholds apply to new/changed code; the full codebase
  is captured once as a baseline (no regression allowed).
- *Control numbers are indicative* - based on the ISO 27002:2022 structure that
  NEN-7510-2:2024 follows. Verify exact numbering against your copy of the standard.
- *OWASP ASVS* is used as the baseline for application security requirements.

---

## Part 1 - Maintainability (ISO 25010)

Each requirement describes exactly one condition. The chain Quality characteristic →
Source code property → Metric follows the SIG/TÜViT model. Measured with SonarCloud +
JaCoCo on new/changed code; the quality gate enforces these and fails CI on a breach.

| ID | ISO 25010 | Source code property (SIG/TÜViT) | Requirement (new code) | Measured via |
|----|-----------|----------------------------------|------------------------|--------------|
| M-01 | Analysability | Unit complexity | Cognitive Complexity ≤ 15 per method | SonarCloud |
| M-02 | Analysability / Modifiability | Unit complexity | Cyclomatic Complexity ≤ 10 per method | SonarCloud |
| M-03 | Analysability / Reusability | Duplication | Duplicated Lines ≤ 3% | SonarCloud |
| M-04 | Testability | Test coverage | Line coverage ≥ 80% | JaCoCo + SonarCloud |
| M-05 | Analysability | Code quality | 0 new Blocker/Critical smells | SonarCloud |
| M-06 | Analysability / Reusability | Unit size | Method length ≤ 30 lines | SonarCloud |
| M-07 | Enforcement | - | Quality Gate "Failed" → CI fails, merge blocked | SonarCloud + branch protection |

Threshold rationale (document this in your analysis):
- CC ≤ 10 aligns with the professor's own classification: CC 1–10 = simple, easy to test.
- Cognitive Complexity captures nested structures better than raw CC.
- 30 lines per method is a widely used unit size threshold; adjust if the baseline scan shows a very different distribution.
- Coverage ≥ 80% applies to new code only (clean as you code principle).
- 0 new Blocker/Critical smells prevents introducing showstopper issues.

**Baseline (record after the first scan):** Lines of Code, smells count (blocker/critical/major),
overall coverage, duplicated lines %, and method-level CC distribution.
Goal: no regression on these values throughout the project.

**Quality gate:** custom gate with "conditions on new code" for M-01–M-06, bound to branch
protection. Document why each threshold was chosen - reference the SIG/TÜViT scale.

---

## Part 2 - Security & Compliance (NEN-7510-2:2024)

### Sprint 1 (wk 5/6) - Project setup & gap analysis

| ID | Requirement | Control |
|----|-------------|---------|
| S-01 | Separate test & production environments (GitHub Environments); separate config + secrets | 8.31, 8.9, 8.24 |
| S-02 | Branch protection rules + approval gates on production deployment | 8.31, 8.2 |
| S-03 | README: environment setup, preventing test data in production, new-developer onboarding | 8.25 |
| S-04 | Gap analysis of 3 chosen controls: current state + actions needed to comply | chosen |

### Sprint 2 (wk 6/7) - Risk analysis, CI/CD, pentest & reporting

| ID | Requirement | Control |
|----|-------------|---------|
| S-05 | CIA analysis: identify crown jewels (with references) and the sensitive data processed | RM |
| S-06 | Risk criteria: scoring scale, risk appetite, thresholds | RM |
| S-07 | Threat model with C4 diagrams (context/container/component); threats identified (e.g. STRIDE) | 8.27, 8.25 |
| S-08 | Risk matrix of identified risks | 8.27 |
| S-09 | Bow-tie for top risks (preventive + corrective controls) | 8.27 |
| S-10 | SAST in the pipeline; build fails above threshold | 8.28, 8.29 |
| S-11 | SCA on dependencies; CVEs drive update advice prioritised by CVSS | 8.8 |
| S-12 | SBOM per build (e.g. CycloneDX) — for NEN-7510 and CRA | 8.8 |
| S-13 | Documented policy for handling false positives | 8.8 |
| S-14 | Risk evaluation of the CI/CD process itself (risk matrix + bow-tie) | 8.25 |
| S-15 | Pentest plan targeting top risks; reproducible, documented findings | 8.29 |
| S-16 | Per finding: justified fix / no-fix decision → security backlog | 8.29, 8.8 |
| S-17 | Prioritised security backlog based on the identified risks | 8.26 |
| S-18 | Risk Assessment Report: scan results + backlog, referenced sensitive data, mitigation per vuln linked to a NEN-7510 control, cost estimate | - |

### Sprint 3 (wk 7) - Attack surface, logging, coverage & re-test

| ID | Requirement | Control |
|----|-------------|---------|
| S-19 | Attack surface map: all entry points + implicit trust, high-risk flagged; threat model updated | 8.26 |
| S-20 | Logging gap analysis vs control 8.15 (event / logged? / sensitive data / compliant); focus on unlogged events | 8.15 |
| S-21 | Complete logging to comply with 8.15; no sensitive data in logs | 8.15 |
| S-22 | Logging tests: successful + failed actions, absence of sensitive data; all pass | 8.15, 8.29 |
| S-23 | Activate code coverage; justify chosen %; coverage report as CI artifact | 8.29 |
| S-24 | Apply mitigations; re-test after updated threat model shows reduced risk; (AI) tooling use justified | 8.29, 8.8 |

### Sprint 4 (wk 8) - Reporting & traceability

| ID | Requirement | Control |
|----|-------------|---------|
| S-25 | Traceability matrix: ≥ 3 controls, each piece of evidence a traceable artifact | cl. 9 |
| S-26 | Audit report (Exec Summary, Scope & Context, Methodology, Risk analysis ≥ 4 findings, SBOM & supply chain, Conclusion & Advice + appendices) | cl. 9 |
| S-27 | CRA mapping: SBOM + vulnerability handling linked to CRA requirements | CRA |
| S-28 | Record items that were not done, with justification | — |

**Control legend** - 8.2 access rights · 8.8 technical vulnerabilities · 8.9 configuration
management · 8.15 logging · 8.24 cryptography/secrets · 8.25 secure SDLC · 8.26 application
security requirements · 8.27 secure architecture · 8.28 secure coding · 8.29 security
testing · 8.31 separation of environments · RM risk management (NEN-7510-1 / ISO 27001
cl. 6) · cl. 9 internal audit · CRA EU Cyber Resilience Act.

---

## Tooling

| Purpose | Tools |
|---------|-------|
| SAST | SonarCloud, CodeQL |
| SCA | OWASP Dependency-Check, Snyk, Dependabot |
| SBOM | Syft / CycloneDX |
| CVE scanning | Trivy |
| Secret scanning | gitleaks, GitHub secret scanning |
| DAST / pentest | OWASP ZAP |
| Threat modelling | OWASP Threat Dragon |
| C4 diagrams | Structurizr, PlantUML (C4-PlantUML) |
| Coverage | JaCoCo |
| Requirements baseline | OWASP ASVS |

---

## Open items

- Choose the 3 controls for the Sprint 1 gap analysis (a logical set: 8.15 logging,
  8.8 vulnerabilities, 8.31 environment separation).
- Fill in the baseline after the first SonarCloud scan, then make the thresholds concrete.
- Keep everything traceable: every finding links to a control and to a platform artifact
  (pipeline step, gate condition, PR review).

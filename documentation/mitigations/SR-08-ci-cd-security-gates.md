# SR-08 - Strengthen CI/CD Security Gates and Branch Protection (#76)

Sprint-3 mitigation record. Documents the changes made for issue #76, the gate demonstration, and any
code changes (per the sprint-3 rule that all code changes are documented).

## Summary

The pipeline already ran CodeQL (SAST), Dependency Review and SBOM/SCA, but most checks were **not
enforced** as required status checks, so a PR could merge with them red. This mitigation makes the
security checks blocking and documents the gate set. Reference policy: `../ci-cd-security-gates.md`.

## Changes made

| Change | Type | Merged to product? |
|--------|------|--------------------|
| Required status checks added on `main`/`dev`: CodeQL (java-kotlin + javascript-typescript), Dependency Review, SBOM & SCA | Branch-ruleset setting | n/a (repo setting) |
| `documentation/ci-cd-security-gates.md` - gate reference + exception process | New doc | Yes |
| This mitigation record | New doc | Yes |

**No product/source code was changed by this issue.** The only repository change is documentation; the
gate enforcement is a branch-ruleset setting.

## Demonstration - gate blocks a high-risk vulnerability

A throwaway branch added a known-vulnerable dependency (`org.apache.logging.log4j:log4j-core:2.14.1` -
Log4Shell, CVE-2021-44228) to the module `pom.xml` and opened a PR into `dev`.

**Result:** the **Dependency Review** gate failed and blocked the PR, flagging two critical and one high
Log4j vulnerability, with the check marked **Required**:

![Dependency Review blocking a vulnerable dependency](../evidence/Dependency%20review%20Demo.png)

The throwaway branch/PR was **closed without merging** - the vulnerable dependency never entered the
product. This satisfies the acceptance criterion *"test case demonstrates a security gate blocking a PR
with a high-risk vulnerability."*

### Secret scanning + push protection

Secret Protection and Push Protection are **enabled** on the repository (see screenshot). Push protection
blocks commits containing **supported partner-pattern** secrets, focusing on verified / high-confidence
detections. A live block using fabricated test secrets (an AWS key and a Google API key) was **not
reproducible** - push protection relies on validity / high-confidence detection rather than pattern
matching alone, so dummy values are not blocked. The control is enabled and would block a real, valid
secret.

![Code scanning, Secret Protection and Push Protection settings](../evidence/Code%20scanning%20sprint%203.png)

### CodeQL SAST (static analysis)

<!-- To be added after the CodeQL demo (Demo B): screenshot of the failing
     "Code scanning results / CodeQL" check + the command-injection (CWE-78) alert. -->

## NEN-7510:2024-2 mapping

- **8.29** (security testing) - failing checks are enforced in the pipeline
- **8.8** (technical vulnerabilities) - Dependency Review blocks new vulnerable dependencies
- **8.32** (change management) - required checks + review make every merge controlled and traceable
- **8.25** (secure development lifecycle) - security activities integrated into the build process

## Status and remaining work

- [x] Required checks set (CodeQL x2, Dependency Review, SBOM & SCA)
- [x] Gates documented (`../ci-cd-security-gates.md`)
- [x] Gate demonstration captured (Dependency Review / Log4Shell)
- [ ] Add **Java CI with Maven / build** as a required check - deferred until #80 merges to `dev`
- [ ] Capture the final branch-ruleset screenshot once the build check is required

## Notes / follow-ups

- The SBOM/SCA gate is intentionally non-blocking on findings (accepted platform-`provided` baseline, see
  `risk-assessment-report.md` §4.3); new vulnerable dependencies are blocked by Dependency Review.
- Minor: the `deny-licenses` option in `dependency-review.yml` is deprecated (noted in the demo output) -
  candidate for a small future cleanup.

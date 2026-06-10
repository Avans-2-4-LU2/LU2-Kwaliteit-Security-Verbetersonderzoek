# Code Review Process

## Purpose

The code review process ensures that all modifications to the codebase are independently evaluated for quality, security, and functional correctness before being integrated. This enforces the four-eyes principle and minimizes the risk of introducing vulnerabilities, bugs, or unauthorized changes into protected branches.

## Configuration

| Setting                    | Value                        |
| -------------------------- | ---------------------------- |
| Review Mechanism           | Pull Request / Merge Request |
| Minimum Required Approvals | 1                            |
| Restrict Deletions         | true                         |
| Administrator Bypass       | Disabled                     |
| Block force push           | true                         |

## Security Rationale

The mandatory review process is a critical component of Secure Software Development. It restricts the ability of any single individual to push code directly to integration or production-ready branches without independent oversight.

Dismissing stale approvals ensures that if new commits are added to a Pull Request after an initial review, the new changes must be re-evaluated. Disabling administrator bypass guarantees that all team members adhere to the same strict development workflow, reinforcing the segregation of duties.

These controls directly support formal change management and traceability, ensuring that every system modification is controlled, authorized, and audited before deployment.

---

# Review Focus Areas

During the review process, developers must validate changes against the following criteria to maintain system integrity:

- **Security:** Absence of vulnerabilities, unsafe code constructs, or hardcoded secrets.
- **Quality:** Adherence to coding standards, maintainability, and adequate test coverage.
- **Functionality:** Alignment with the defined system requirements and acceptance criteria.
- **Compliance:** Verification that the changes adhere to secure software development principles described in NEN 7510.

This structured verification acts as the primary defense against operational risks and ensures continuous compliance throughout the software lifecycle.

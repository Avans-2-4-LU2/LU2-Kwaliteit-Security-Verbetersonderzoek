# Branch Protection Rules

## Purpose

The branch protection rules ensure the stability and security of critical branches (e.g., `main` or release branches) by preventing accidental or malicious direct changes. These rules enforce standardized workflows, mandating that all modifications go through established pull request and review processes.

## Configuration

| Setting                             | Value     |
| ----------------------------------- | --------- |
| Protected Branches                  | main, dev |
| Require Pull Request before merging | true      |
| Require approvals                   | 1         |
| Restrict branch deletions           | true      |
| Block force pushes                  | true      |

## Security Rationale

Branch protection is a fundamental control for secure software development. It guarantees that critical codebases cannot be arbitrarily modified by unreviewed or unauthorized commits.

By requiring pull requests and blocking force pushes, the repository maintains an immutable and traceable history of all changes. Restricting deletions prevents the accidental or malicious loss of source code history. Requiring status checks ensures that automated tests and security scans must pass before any code is merged, preventing broken or insecure code from reaching production or integration environments.

These mechanisms align with the principles of segregation of duties and secure change management, ensuring that all modifications are deliberate, tested, independently reviewed, and fully audited.

---

# Protection Focus Areas

When configuring and maintaining branch protection, the following criteria are essential to enforce system integrity:

- **Integrity:** Preventing unauthorized rewriting of Git history (e.g., blocking force pushes).
- **Quality Assurance:** Mandating successful CI/CD pipelines, security scans, and automated testing prior to code integration.
- **Traceability:** Ensuring all code changes are linked to a documented review process and can be audited.
- **Compliance:** Enforcing change control policies in line with secure software development standards, such as NEN 7510.

This framework acts as the primary safeguard for the source of truth, mitigating risks of operational instability and ensuring a reliable, compliant path to production.

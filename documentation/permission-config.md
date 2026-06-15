# Deployment Permission Configuration

## Purpose

The deployment permission configuration enforces Role-Based Access Control (RBAC) and the Principle of Least Privilege across all environments. This control ensures that only authorized personnel can trigger, approve, or execute deployments to critical environments, minimizing the risk of unauthorized or accidental releases.

## Configuration

| Setting                         | Value                          |
| ------------------------------- | ------------------------------ |
| Test Environment Access         | Developers                     |
| Production Environment Access   | Sander / Mathijs               |
| Production Deployment Execution | Restricted to Authorized Roles |
| Required Approvals (Prod)       | 1 (Authorized Role)            |

## Security Rationale

Strict permission controls are vital for maintaining the segregation of duties between development and release management. By restricting production deployment approvals and execution to specific authorized users, the risk of insider threats or accidental misconfigurations is significantly reduced.

Enforcing the Principle of Least Privilege guarantees that team members only have the access necessary for their specific roles. Removing legacy or overly permissive access rights directly supports formal change management and traceability, ensuring that every deployment is controlled, authorized, and fully audited.

These configurations adhere to secure software development standards and ensure accountability for all environment modifications.

---

# Access Control Focus Areas

When defining and reviewing permission configurations, focus on the following criteria to maintain a secure deployment pipeline:

- **Segregation of Duties:** Ensuring developers cannot unilaterally deploy code to production environments.
- **Least Privilege:** Limiting access rights strictly to the environments and actions necessary for a user's role.
- **Traceability:** Guaranteeing that all deployment approvals and executions are logged and tied to specific, authorized identities.
- **Compliance:** Verifying that access controls align with organizational security policies and standards, such as NEN 7510.

This structured approach to permissions safeguards critical infrastructure and ensures that all releases are predictable, secure, and compliant.

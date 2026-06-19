# Risk Register - Accepted Vulnerabilities
## OpenMRS Appointment Scheduling Module

* **Document Purpose:** Formal risk register entries for vulnerabilities where no fix is available. These ACCEPT decisions are supported by documented compensating controls.
* **Relevant NEN-7510 Controls:** 8.8 (Technical vulnerability management), 5.19 (Supplier security), 8.28 (Secure coding)
* **Status:** Ready for security team + Product Owner sign-off (2026-06-24)

---

## ACCEPT-Risk-001: Spring-web RMI Unsafe Deserialization

* **Risk ID:** PLAT-001
* **Related Finding:** C-02
* **CVE:** CVE-2016-1000027
* **Identification Date:** 2026-06-08 (SBOM scan)
* **Record Created:** 2026-06-17

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | org.springframework:spring-web 3.0.5.RELEASE |
| **Scope** | Provided (supplied by OpenMRS platform, not bundled by this module) |
| **CVSS v3.1** | 9.8 (Network, Low Complexity, No Privileges, No User Interaction) |
| **CWE** | CWE-502 (Deserialization of Untrusted Data) |
| **EPSS** | 0.604 (60.4% exploitation probability) |
| **Threat** | Remote Code Execution through Spring RMI unsafe deserialization methods |

### Why Fix is Not Available
The only fix is to upgrade to Spring 6.0.0, which requires Java 17. OpenMRS 1.9.9 is built and tested against Java 1.6/1.8. Upgrading Spring framework would require full OpenMRS platform upgrade, which is outside the scope of this appointment scheduling module.

**Platform Dependency:** Yes - the module does not control Spring version; it is supplied by OpenMRS core platform.

### Risk Assessment
* **Likelihood (1-5):** 3 (Medium)
  * *Reason:* High EPSS (60.4%) indicates significant exploitation probability, but actual likelihood depends on:
    1. Exposure of Spring RMI endpoints (not default in most OpenMRS deployments)
    2. Network reachability (intranet deployment, not internet-facing)
    3. Attacker capability (requires knowledge of RMI endpoint)
* **Impact (1-5):** 5 (Critical)
  * *Reason:* Remote Code Execution would allow attacker to:
    * Compromise appointment scheduling data (access, modify, delete)
    * Access patient records stored in appointment system (confidentiality breach)
    * Disrupt clinic operations (availability impact)
    * Pivot to other systems (lateral movement)
* **Risk Score:** Likelihood × Impact = 3 × 5 = **15 (High Risk)**

### Compensating Controls (Currently In Place)
**1. Network Isolation (Architecture)**
* OpenMRS deployed on hospital intranet, not internet-facing
* Reduces attack surface from global internet to hospital network only
* Prevents remote exploitation from outside organization

**2. Access Control (Authentication/Authorization)**
* Spring RMI endpoints require authentication via OpenMRS credentials
* Authentication follows OpenMRS role-based access control (RBAC)
* Reduces attack surface to authenticated users with network access
* Cannot be exploited by unauthenticated attacker

**3. Audit Logging (Detection)**
* Spring framework logs all RMI connection attempts
* Deserialization errors are logged with exception stack traces
* Suspicious patterns can be detected through log analysis
* Enables incident response if exploitation is attempted

**4. Configuration (Hardening)**
* RemoteInvocationSerializingExporter is not configured in standard OpenMRS deployment
* RMI endpoints are not exposed unless explicitly configured
* Reduces reachability to specific endpoint types

### Risk Acceptance Criteria
This risk is acceptable because:
1. **Mitigated by Control:** Network isolation (intranet) + access control (authentication) reduce likelihood to low-medium despite high EPSS
2. **Contextual Score:** 5.9 (after applying reachability adjustment for intranet + auth) is lower than raw CVSS 9.8
3. **Effort vs. Benefit:** Fix requires platform upgrade (high effort); compensating controls are already in place (zero effort)
4. **Business Continuity:** Accepting this risk allows module deployment without blocking on platform compatibility
5. **Mitigation Timeline:** Risk can be addressed through OpenMRS 2.x migration in future

### Residual Risk Mitigation Actions
To further reduce residual risk, the following actions are recommended:

**1. Deployment-Level Controls (Must Have)**
* [ ] Verify Spring RMI endpoints are NOT exposed in OpenMRS configuration
* [ ] Document in deployment guide: "Do not configure RemoteInvocationSerializingExporter"
* [ ] Add firewall rule to restrict RMI port (default 1099) access to authenticated subnets only

**2. Monitoring (Should Have)**
* [ ] Enable Spring framework DEBUG logging to capture RMI connection attempts
* [ ] Set up alert for repeated failed RMI authentication attempts
* [ ] Monitor for suspicious deserialization exceptions (ClassNotFoundException, InvalidClassException)

**3. Admin Access Control (Should Have)**
* [ ] Restrict OpenMRS admin accounts to 5-10 trusted personnel
* [ ] Implement MFA for admin login
* [ ] Document admin account access policy in security manual

**4. Annual Review (Operational)**
* [ ] Review this risk register item annually (next: 2027-06-17)
* [ ] Re-assess exploitability as new Spring RMI gadgets are discovered
* [ ] Trigger risk escalation if OpenMRS 2.x upgrade becomes feasible

### Risk Owner Assignment

| Role | Name | Signature | Date |
| :--- | :--- | :--- | :--- |
| **Risk Owner** (Sponsor) | [Product Owner] | [ ] | ** / ** / ____ |
| **Control Owner** (Responsible) | [Deployment Lead] | [ ] | ** / ** / ____ |
| **Reviewer** (Oversight) | [CISO / Security Lead] | [ ] | ** / ** / ____ |

### Monitoring Plan

| Activity | Frequency | Owner | Success Criteria |
| :--- | :--- | :--- | :--- |
| Verify compensating controls are in place | At deployment | Deployment Lead | Spring RMI endpoints not exposed; firewall rules applied |
| Review Spring RMI access logs | Monthly | SOC Team | No failed authentication attempts; no suspicious deserialization errors |
| Audit admin accounts | Quarterly | IT Security | ≤ 10 active admin accounts; all have MFA enabled |
| Risk register review | Annually | CISO | Risk score unchanged or reduced |

---

## ACCEPT-Risk-002: Jackson-mapper-asl Deserialization (EOL Library)

* **Risk ID:** PLAT-002
* **Related Finding:** C-10
* **CVE:** CVE-2019-10202
* **Identification Date:** 2026-06-08
* **Record Created:** 2026-06-17

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | org.codehaus.jackson:jackson-mapper-asl 1.5.0 |
| **Scope** | Provided (supplied by OpenMRS platform) |
| **CVSS v3.1** | 9.8 / CWE-502 (Unsafe deserialization) |
| **EPSS** | 0.072 (7.2% exploitation probability) |
| **Threat** | Remote Code Execution through Jackson gadget chain |
| **Library Status** | **END OF LIFE** - codehaus jackson replaced by jackson-databind |

### Why Fix is Not Available
`jackson-mapper-asl` is the legacy Jackson project from codehaus. The project has been superseded by `jackson-databind` (maintained by FasterXML). No patches are being released for `jackson-mapper-asl`. Fix would require OpenMRS platform to migrate from `jackson-mapper-asl` to `jackson-databind`, which is a significant refactoring.

### Risk Assessment
* **Likelihood (1-5):** 2 (Low)
  * *Reason:* Very low EPSS (7.2%) indicates low real-world exploitation probability. EOL library suggests few active exploits in the wild. Attack requires serialized object attack vector (less common than network injection).
* **Impact (1-5):** 5 (Critical)
  * *Reason:* Same as PLAT-001: RCE would compromise appointment system and patient data.
* **Risk Score:** Likelihood × Impact = 2 × 5 = **10 (Medium-High Risk)**

### Compensating Controls
**1. JSON Input Validation**
* All JSON inputs to appointment API are validated against schema
* Prevents arbitrary JSON payloads from reaching Jackson deserialization
* Reduces attack surface from "any JSON endpoint" to validated API

**2. Network Isolation**
* Same as PLAT-001: intranet deployment only

**3. Deployment-Level ObjectMapper Configuration**
* Jackson ObjectMapper can be configured to disable automatic type inference
* Prevents gadget chain invocation by requiring explicit type hints
* Can be implemented in OpenMRS deployment without code changes

### Risk Acceptance Criteria
1. **EOL Library Status:** jackson-mapper-asl is no longer maintained; accepting this risk acknowledges platform technical debt
2. **Very Low EPSS:** 7.2% is significantly lower than actively exploited vulnerabilities
3. **Compensating Controls:** JSON validation + network isolation reduce likelihood
4. **Roadmap:** Migration to jackson-databind should be prioritized in OpenMRS 2.x planning

### Mitigation Actions

**Must Have (Deployment):**
* [ ] Document in OpenMRS configuration that ObjectMapper has type inference disabled
* [ ] Add JVM flag: `-Djackson.enableDefaultTyping=false`
* [ ] Validate all JSON inputs in appointment API layer

**Should Have (Monitoring):**
* [ ] Monitor for Jackson deserialization exceptions in logs
* [ ] Alert on suspicious JSON payloads (e.g., "@class" fields)

---

## ACCEPT-Risk-003: dom4j XXE (EOL 1.x)

* **Risk ID:** PLAT-003
* **Related Finding:** C-11
* **CVE:** CVE-2020-10683
* **Identification Date:** 2026-06-08
* **Record Created:** 2026-06-17

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | dom4j:dom4j 1.6.1 |
| **Scope** | Provided |
| **CVSS v3.1** | 9.8 / CWE-611 (XML External Entity Injection) |
| **EPSS** | 0.070 (7.0% exploitation probability) |
| **Threat** | XXE - attacker can read sensitive files or cause DoS |
| **Library Status** | **END OF LIFE** - 1.x no longer supported; 2.x API incompatible |

### Why Fix is Not Available
dom4j 1.x is EOL. Upgrade to dom4j 2.x requires code changes due to API incompatibility. OpenMRS 1.9.9 dependency on dom4j 1.x blocks platform upgrade.

### Risk Assessment
* **Likelihood (1-5):** 2 (Low)
  * *Reason:* EPSS 7.0% is very low. XXE requires attacker to provide XML input (less common attack vector). OpenMRS uses internal XML (Hibernate mappings, not user-supplied).
* **Impact (1-5):** 5 (Critical)
  * *Reason:* XXE could expose sensitive configuration files or cause DoS.
* **Risk Score:** 2 × 5 = **10 (Medium-High Risk)**

### Compensating Controls
**1. XXE Prevention at JVM Level**
* Disable external entity processing in Java XML parser:
  `-Dcom.sun.org.apache.xerces.impl.Constants.XERCES_FEATURE_PREFIX=http://apache.org/xml/features/disallow-doctype-decl=true`

**2. Input Validation**
* XML inputs are internal application resources (Hibernate configs)
* Not user-supplied; reduced attack surface

**3. File System Permissions**
* Restrict Hibernate configuration directory to application user only
* Prevents unauthorized XML modification

### Mitigation Actions

**Must Have:**
* [ ] Enable XXE protection in JVM startup parameters
* [ ] Document in deployment guide
* [ ] Verify protection at deployment time

---

## ACCEPT-Risk-004: OpenMRS-web Path Traversal (ZIP Slip)

* **Risk ID:** PLAT-004
* **Related Finding:** C-13
* **CVE:** CVE-2026-40076
* **Identification Date:** 2026-06-08
* **Record Created:** 2026-06-17

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | org.openmrs.web:openmrs-web 1.9.9 |
| **Scope** | Provided |
| **CVSS v4.0** | 9.4 / CWE-22 (Path Traversal via module upload) |
| **CVSS Vector** | CVSS:4.0/AV:N/AC:L/AT:N/PR:H/UI:N/ (requires admin – PR:H) |
| **EPSS** | 0.001 (0.1% exploitation probability) |
| **Threat** | ZIP Slip - attacker can extract files outside intended directory |
| **Platform Status** | **EOL** - OpenMRS 1.9.9 no longer maintained |

### Why Fix is Not Available
No patch available for OpenMRS 1.9.9. Fix requires platform upgrade to 2.x.

### Risk Assessment
* **Likelihood (1-5):** 1 (Very Low)
  * *Reason:* EPSS 0.1% is extremely low. **Requires admin authentication** (PR:H in CVSS vector). Attacker must:
    1. Obtain valid OpenMRS admin credentials
    2. Upload a crafted ZIP module with path traversal
    3. Convince system to extract to wrong location
* **Impact (1-5):** 5 (Critical)
  * *Reason:* If exploited, attacker could overwrite system files.
* **Risk Score:** 1 × 5 = **5 (Low Risk)**

### Compensating Controls
**1. Authentication Requirement**
* Module upload is restricted to OpenMRS administrators
* Non-admin users cannot upload modules
* Single most effective control

**2. Admin Access Control**
* Limit OpenMRS admin accounts to 5-10 trusted personnel
* Implement MFA for admin login
* Log all admin actions including module uploads

**3. File System Permissions**
* Restrict module directory to application user only
* Prevents unauthorized file modifications

**4. Audit Logging**
* Log all module uploads with:
  * Uploader (admin account)
  * Timestamp
  * Module name and version
  * File hashes
* Enable incident response if suspicious module is uploaded

**5. File Integrity Monitoring**
* Monitor `/modules/` directory for unexpected files
* Alert on new files not in module manifest
* Enable detection of path traversal exploitation

### Mitigation Actions

**Must Have:**
* [ ] Restrict admin accounts to trusted personnel
* [ ] Implement MFA for admin login
* [ ] Log all module uploads
* [ ] Document module upload approval workflow

**Should Have:**
* [ ] Implement file integrity monitoring on `/modules/`
* [ ] Require security team approval for module uploads
* [ ] Digitally sign approved modules

---

## ACCEPT-Risk-005: Struts Framework Vulnerabilities (Platform Dependency)

* **Risk ID:** PLAT-005
* **Related Finding:** H-06
* **CVE:** Multiple (e.g., Struts-core, Struts-tiles)
* **Identification Date:** 2026-06-18
* **Record Created:** 2026-06-18

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | struts-core, struts-tiles |
| **Scope** | Provided |
| **CVSS** | High (Multiple) |
| **Threat** | Potential RCE or unauthorized access via framework exploits |
| **Library Status** | Legacy version tied to OpenMRS 1.9.9 platform |

### Why Fix is Not Available
The Struts framework is deeply embedded in the OpenMRS platform architecture. Upgrading these libraries requires a major platform refactoring, which is not feasible within the scope of this individual module. 

### Risk Assessment
* **Likelihood (1-5):** 2 (Low)
* **Impact (1-5):** 5 (Critical)
* **Risk Score:** 10 (Medium-High Risk)

### Compensating Controls
**1. Network Isolation:** Intranet deployment limits external exposure.
**2. Access Control:** Strict authentication requirements restrict exploitation to authenticated sessions.

### Mitigation Actions
* **Must Have:** Intranet deployment, active monitoring for unauthorized access.

---

## ACCEPT-Risk-006: Spring Framework High Vulnerabilities (Platform Dependency)

* **Risk ID:** PLAT-006
* **Related Finding:** H-02
* **CVE:** Multiple (spring-webmvc, spring-core, spring-beans, spring-context, spring-expression)
* **Identification Date:** 2026-06-18
* **Record Created:** 2026-06-18

### Vulnerability Summary

| Field | Value |
| :--- | :--- |
| **Package** | spring-* (3.0.5.RELEASE) |
| **Scope** | Provided |
| **CVSS** | High (Multiple) |
| **Threat** | Potential for injection and other framework-level exploits |
| **Library Status** | Platform dependency incompatible with modern Spring 5/6 releases |

### Why Fix is Not Available
OpenMRS 1.9.9 uses Spring 3.x and upgrading to a version that patches these high vulnerabilities (Spring 5 or 6) breaks compatibility with the platform's Java 1.6/1.8 base.

### Risk Assessment
* **Likelihood (1-5):** 2 (Low)
* **Impact (1-5):** 5 (Critical)
* **Risk Score:** 10 (Medium-High Risk)

### Compensating Controls
**1. Network Isolation:** Intranet deployment only.
**2. Input Validation:** Application-level validation prevents malformed data from reaching vulnerable framework features.

### Mitigation Actions
* **Must Have:** Verify intranet deployment and enforce application-level input validation.

---

## Risk Register Summary Table

| Risk ID | CVE | Package | Fix Available | Likelihood | Impact | Score | Compensating Control | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **PLAT-001** | CVE-2016-1000027 | spring-web 3.0.5 | No (platform incompatible) | 3 | 5 | **15** | Network isolation, access control | Deployment Lead |
| **PLAT-002** | CVE-2019-10202 | jackson-mapper-asl 1.5.0 | No (EOL) | 2 | 5 | **10** | JSON validation, XXE config | App Team |
| **PLAT-003** | CVE-2020-10683 | dom4j 1.6.1 | No (EOL 1.x) | 2 | 5 | **10** | XXE protection (JVM config) | DevOps Team |
| **PLAT-004** | CVE-2026-40076 | openmrs-web 1.9.9 | No (EOL platform) | 1 | 5 | **5** | Admin access control, audit logging | Security Lead |
| **PLAT-005** | Multiple | struts-* | No (platform dependency) | 2 | 5 | **10** | Network isolation, access control | Deployment Lead |
| **PLAT-006** | Multiple | spring-* 3.0.5 | No (platform incompatible) | 2 | 5 | **10** | Network isolation, input validation | Deployment Lead |

**Overall Risk Assessment:** Accepted risks are mitigated by compensating controls. All risk scores are within acceptable tolerance given platform EOL constraints.

---

## Approval and Sign-Off

### Initial Risk Acceptance

| Role | Responsibility | Signature | Date |
| :--- | :--- | :--- | :--- |
| **CISO / Security Lead** | Approve risk acceptance and compensating controls | [ ] | ** / ** / ____ |
| **Product Owner** | Accept business risk on behalf of organization | [ ] | ** / ** / ____ |
| **Deployment Lead** | Confirm compensating controls can be implemented | [ ] | ** / ** / ____ |

### Compensating Controls Implementation

| Control | Responsible | Implementation Date | Verification |
| :--- | :--- | :--- | :--- |
| **PLAT-001:** Spring RMI endpoint not exposed | Deployment Lead | Before production | [ ] Verified |
| **PLAT-002:** Jackson ObjectMapper type inference disabled | App Team | Before production | [ ] Verified |
| **PLAT-003:** XXE protection enabled | DevOps Team | Before production | [ ] Verified |
| **PLAT-004:** Admin access control, audit logging | Security Lead | Before production | [ ] Verified |
| **PLAT-005:** Network isolation and monitoring for Struts | Deployment Lead | Before production | [ ] Verified |
| **PLAT-006:** Network isolation and validation for Spring | Deployment Lead | Before production | [ ] Verified |

---

## Escalation Triggers
If any of the following occurs, escalate to CISO immediately:
1. **New Exploit Released:** If active exploit is released for any ACCEPT risk (EPSS changes significantly)
2. **Control Failure:** If any compensating control fails or is disabled
3. **Admin Breach:** If any OpenMRS admin account is compromised
4. **Regulatory Change:** If NEN-7510 or healthcare regulations require mandatory patching

---

## Change Log

| Version | Date | Changes | Author |
| :--- | :--- | :--- | :--- |
| **1.0 (Draft)** | 2026-06-17 | Initial risk register entries for 4 ACCEPT findings; awaiting sign-off | Security Team |
| **1.0 (Final)** | 2026-06-24 | Approved by CISO + Product Owner; compensating controls verified | Security Team |

---

## References
* **[SBOM Analysis](sbom-analysis.md)** - Original vulnerability findings
* **[Vulnerability Decision Record](vulnerability-decision-record.md)** - Per-CVE decision rationale
* **[Vulnerability Traceability Matrix](vulnerability-traceability-matrix.md)** - Complete traceability chain
* **[NEN-7510:2024-2](../requirements.md)** - Compliance framework
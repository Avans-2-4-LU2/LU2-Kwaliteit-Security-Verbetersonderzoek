# OpenMRS Appointment Scheduling Module - Quality & Security Improvement Project

## Table of Contents

- [Project Overview](#project-overview)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Local Setup & Installation](#local-setup--installation)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Development Tools & Quality Checks](#development-tools--quality-checks)
- [Branching Strategy](#branching-strategy)
- [Code Review Process](#code-review-process)
- [Contributing](#contributing)
- [Security & Compliance](#security--compliance)
- [Troubleshooting](#troubleshooting)
- [Resources](#resources)

---

## Project Overview

This repository contains the **OpenMRS Appointment Scheduling Module** with a comprehensive quality and security improvement initiative aligned with **NEN-7510-2:2024** compliance standards.

### Project Objectives

The project focuses on two primary initiatives:

1. **Maintainability (Part 1):** Enforcing strict code quality standards using SonarQube, static analysis (SAST), and clean code principles.
2. **Security & Compliance (Part 2):** Implementing security controls, dependency scanning (SCA), threat modeling, and secure software development practices.

### Key Features

The Appointment Scheduling Module provides:

- **Patient Appointment Management:** Schedule, update, and track patient appointments
- **Provider Schedule Management:** Manage provider availability and scheduling rules
- **Appointment Types & Blocks:** Define appointment types and provider time blocks
- **Integration:** Seamless integration with OpenMRS 1.9.9+ using Web Services REST API

### Module Information

- **Artifact ID:** `appointmentscheduling`
- **Version:** 1.17.0-SNAPSHOT
- **Organization:** OpenMRS
- **OpenMRS Compatibility:** 1.9.9+

---

## Prerequisites

Before setting up the project locally, ensure you have the following installed:

### Required

- **Java Development Kit (JDK) 8+**
  - Verify installation: `java -version`
  - Download: [OpenJDK](https://openjdk.java.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)

- **Apache Maven 3.6+**
  - Verify installation: `mvn -version`
  - Download: [Maven Downloads](https://maven.apache.org/download.cgi)

- **Git**
  - Verify installation: `git --version`
  - Download: [Git](https://git-scm.com/)

### Optional but Recommended

- **OpenMRS Platform 1.9.9+** (for local development and testing)
  - Installation instructions: [OpenMRS Setup](https://wiki.openmrs.org/display/docs/OpenMRS+1.9.x+Installation)

- **IDE:** IntelliJ IDEA, Eclipse, or VS Code with Java extensions

- **Docker** (for containerized testing and CI/CD)

---

## Project Structure

```
.
├── README.md                                      # This file
├── documentation/                                 # Project documentation
│   ├── branch-protection-documentation.md         # Git branch protection rules
│   ├── commit-convention.md                       # Git commit message standards
│   ├── dependency-review.md                       # Dependency management guidelines
│   ├── environment-configuration.md               # Environment setup and secrets
│   ├── permission-config.md                       # User role and permission configuration
│   ├── Requirements.md                            # Functional & non-functional requirements
│   ├── review-documentation.md                    # Code review process guidelines
│   └── sbom.md                                    # Software Bill of Materials (SBOM)
│
├── openmrs-module-appointmentscheduling/          # Main module
│   ├── pom.xml                                    # Parent POM with dependencies and build config
│   ├── api/                                       # API module (core business logic)
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/org/openmrs/...         # Source code
│   │       └── test/java/org/openmrs/...         # Unit & integration tests
│   │
│   ├── omod/                                      # Web module (UI and web services)
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/org/openmrs/...         # Web controllers and handlers
│   │       └── webapp/                           # JSP pages and web resources
│   │
│   └── sca-report/                                # Security scanning reports
│       └── grype-report.json                      # Software Composition Analysis results
```

### Module Responsibilities

- **api:** Contains domain models, business logic, database mappings (Hibernate), and service layer
- **omod:** Provides web controllers, REST endpoints, UI views (JSP), and web service configuration

---

## Local Setup & Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/openmrs/openmrs-module-appointmentscheduling.git
cd openmrs-module-appointmentscheduling
```

### Step 2: Set Up Your Development Environment

**Configure your IDE:**

- **IntelliJ IDEA:**
  1. Open the project root
  2. IntelliJ will detect the Maven project structure
  3. Allow it to download dependencies
  4. Set Project SDK to JDK 8+ (File → Project Structure → Project)

- **Eclipse:**
  1. File → Import → Existing Maven Projects
  2. Select the project root
  3. Eclipse will configure the Maven project

- **VS Code:**
  1. Install Extension Pack for Java
  2. Open the project folder
  3. VS Code will auto-detect and configure the Maven project

### Step 3: Build the Project

```bash
# Clean and build the entire project (api + omod)
mvn clean install

# Build with skipping tests (faster for initial setup)
mvn clean install -DskipTests

# Build a specific module
cd api
mvn clean install
cd ../omod
mvn clean install
```

**Expected output:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
[INFO] Finished at: YYYY-MM-DDTHH:MM:SS+01:00
```

### Step 4: Install OpenMRS Platform (Optional)

For local testing and development:

```bash
# Follow OpenMRS Platform setup: https://wiki.openmrs.org/display/docs/OpenMRS+1.9.x+Installation
# The appointment scheduling module can be deployed as a plugin
```

### Step 5: Environment Configuration

**Create environment-specific configuration:**

See [documentation/environment-configuration.md](documentation/environment-configuration.md) for detailed configuration of:

- Test environment variables
- Production environment variables
- Secret management (credentials, API keys)

**Key environment variables:**

```bash
# Test environment
export ENVIRONMENT=test
export DATABASE_URL=jdbc:mysql://localhost:3306/openmrs_test
export ACTIVE_PROFILE=test

# Production environment
export ENVIRONMENT=production
export DATABASE_URL=jdbc:mysql://prod-db-host:3306/openmrs
export ACTIVE_PROFILE=production
```

⚠️ **Important:** Never commit secrets or production credentials to the repository. Use GitHub Secrets or environment-specific configuration files excluded from version control.

---

## Running the Application

### Prerequisites for Running

1. OpenMRS Platform 1.9.9+ must be installed and running
2. MySQL or compatible database server must be accessible
3. All dependencies built successfully (see [Local Setup](#local-setup--installation))

### Start the Application

**If using OpenMRS Platform:**

```bash
# Deploy the built module to OpenMRS
# Copy the generated .omod file to OpenMRS modules directory
cp openmrs-module-appointmentscheduling/omod/target/appointmentscheduling-1.17.0-SNAPSHOT.omod \
   $OPENMRS_HOME/modules/

# Start OpenMRS
$OPENMRS_HOME/tomcat/bin/catalina.sh run
```

**Access the module:**

- Navigate to: `http://localhost:8080/openmrs/`
- Login with valid OpenMRS credentials
- Access Appointment Scheduling: Administration → Modules → Appointment Scheduling

### Verify Installation

```bash
# Check if module is loaded
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/module \
  -u admin:admin | grep appointmentscheduling

# Expected output includes the appointmentscheduling module with status "started"
```

---

## Running Tests

The project includes comprehensive unit and integration tests using **JUnit** and **Mockito**.

### Run All Tests

```bash
# Execute all tests in the project
mvn test

# Run tests with coverage reporting
mvn test jacoco:report
# Coverage report location: target/site/jacoco/index.html
```

### Run Tests for a Specific Module

```bash
# Run API module tests only
cd api
mvn test

# Run web module tests only
cd omod
mvn test
```

### Run a Specific Test Class

```bash
mvn test -Dtest=AppointmentServiceTest
mvn test -Dtest=AppointmentServiceTest#testGetAppointmentsByPatient
```

### Run Tests with Different Profiles

```bash
# Run tests with test profile
mvn test -P test

# Run tests with production profile (integration tests)
mvn test -P integration
```

### Test Reports

Test results are generated in standard locations:

```
api/target/surefire-reports/          # API module test results
omod/target/surefire-reports/         # Web module test results
target/site/jacoco/index.html         # Code coverage report
```

View test coverage:

```bash
mvn jacoco:report
# Open: target/site/jacoco/index.html
```

---

## Development Tools & Quality Checks

### Code Quality Analysis

#### SonarQube Static Analysis (SAST)

```bash
# Run SonarQube analysis
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=appointmentscheduling \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<YOUR_SONAR_TOKEN>
```

#### Code Style Formatting

The project includes an OpenMRS code formatter (`OpenMRSFormatter.xml`):

```bash
# Format code in IntelliJ IDEA:
# IDE → Code → Reformat Code
# or: Ctrl+Alt+L (Windows/Linux) / Cmd+Option+L (Mac)

# Use Eclipse formatter:
# Project → Properties → Java Code Style → Formatter
# Import OpenMRSFormatter.xml profile
```

### Dependency Scanning (SCA)

```bash
# Check for vulnerable dependencies using Maven
mvn org.owasp:dependency-check-maven:check

# Software Composition Analysis (SCA) results available in:
# sca-report/grype-report.json
```

### Static Code Analysis

The repository does not currently include explicit PMD, Checkstyle, or FindBugs plugin configuration in the root Maven POM. If you choose to add these tools, their commands can be run from the project root.

---

## Branching Strategy

The project follows a structured Git workflow to ensure code quality and production stability.

### Branch Overview

| Branch      | Purpose                             | Protection     |
| ----------- | ----------------------------------- | -------------- |
| `main`      | Production-ready, released code     | ✅ Protected   |
| `dev`       | Integration and testing branch      | ✅ Protected   |
| `feature/*` | Individual feature/task development | ❌ Unprotected |

For detailed branching strategy, see [documentation/environment-configuration.md](documentation/environment-configuration.md).

### Workflow

1. **Create a feature branch** from `dev`:

   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feature/TASK-123-description
   ```

2. **Implement changes** and commit with [conventional commits](documentation/commit-convention.md):

   ```bash
   git add .
   git commit -m "feat: add appointment scheduling for providers"
   # or
   git commit -m "fix: resolve null pointer in appointment validation"
   # or
   git commit -m "docs: update README with setup instructions"
   ```

3. **Push to remote**:

   ```bash
   git push origin feature/TASK-123-description
   ```

4. **Create Pull Request** to `develop`:
   - Provide clear description of changes
   - Link to related issue/task
   - Ensure CI/CD checks pass

5. **Code Review** (minimum 1 approval required):
   - Address review comments
   - Push fixes to the same branch
   - Re-request review after updates

6. **Merge to develop**:

   ```bash
   # After approval, GitHub will show "Merge" button
   # Click to merge (using "Squash and merge" recommended)
   ```

7. **Release to main** (validated changes only):
   ```bash
   git checkout main
   git pull origin main
   git merge develop --no-ff
   git push origin main
   ```

### Commit Conventions

Follow [documentation/commit-convention.md](documentation/commit-convention.md) for standardized commit messages:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Examples:**

```
feat(scheduler): add provider availability validation
fix(api): resolve appointment overlap detection bug
docs: update appointment workflow documentation
test: add edge case tests for appointment creation
```

---

## Code Review Process

The code review process ensures quality, security, and compliance before code integration.

### Review Requirements

- **Minimum Reviewers:** 1 approval required
- **Stale Review Dismissal:** Enabled (new commits require re-review)
- **Status Checks:** All CI/CD checks must pass
- **Administrator Bypass:** Disabled (applies to all team members)

### Review Focus Areas

During code review, verify:

1. **Security:**
   - No hardcoded secrets or credentials
   - No SQL injection or other injection vulnerabilities
   - Proper input validation and output encoding
   - Secure authentication and authorization

2. **Quality:**
   - Code follows project style guide (OpenMRSFormatter.xml)
   - Logic is clear and maintainable
   - Adequate test coverage (minimum 70% new code)
   - No code duplication

3. **Functionality:**
   - Changes align with requirements
   - No regression in existing features
   - Backward compatibility maintained

4. **Compliance:**
   - Adherence to NEN-7510 secure development principles
   - Proper logging and audit trails
   - Documentation updated if needed

For detailed guidance, see [documentation/review-documentation.md](documentation/review-documentation.md).

### Example Review Checklist

```markdown
- [ ] Code builds successfully
- [ ] Tests pass (100% pass rate)
- [ ] Code coverage maintained or improved
- [ ] No security vulnerabilities introduced
- [ ] Follows coding standards and conventions
- [ ] Documentation updated
- [ ] Backward compatibility maintained
- [ ] Accidental test/debug code removed
```

---

## Contributing

We welcome contributions! Please follow these guidelines:

### Before You Start

1. **Check Issues:** Review existing issues to avoid duplicate work
2. **Discuss Major Changes:** Open an issue to discuss significant changes before implementing
3. **Understand Requirements:** Read [documentation/Requirements.md](documentation/Requirements.md)

### Contribution Process

1. **Fork or Branch:**

   ```bash
   git checkout -b feature/your-feature develop
   ```

2. **Implement Your Changes:**
   - Follow coding standards
   - Write tests for new functionality
   - Update documentation

3. **Run Local Quality Checks:**

   ```bash
   # Build
   mvn clean install

   # Run all checks
   mvn clean verify

   # Run tests with coverage
   mvn test jacoco:report
   ```

4. **Commit with Conventional Messages:**

   ```bash
   git commit -m "feat(scheduler): implement feature X"
   ```

5. **Submit Pull Request:**
   - Provide clear description
   - Reference related issues
   - Include test evidence
   - Link to requirements

6. **Respond to Review Feedback:**
   - Address all comments
   - Re-request review when ready

### Code Standards

- **Java Version:** 8+
- **Code Formatter:** [OpenMRSFormatter.xml](openmrs-module-appointmentscheduling/OpenMRSFormatter.xml)
- **Naming Conventions:** Follow OpenMRS conventions
- **Test Coverage:** Minimum 70% for new code
- **Documentation:** Update Javadoc and README as needed

### Testing Requirements

- All tests must pass
- New features must include unit tests
- Test code must follow the same standards as production code
- Coverage reports must show adequate coverage

### Reporting Issues

When reporting issues, include:

```markdown
**Description:**
Clear description of the issue

**Steps to Reproduce:**

1. ...
2. ...
3. ...

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Environment:**

- OS: [Windows/Mac/Linux]
- Java Version: [8.0/11/17/etc]
- Maven Version: [3.6/3.8/etc]
- OpenMRS Version: [1.9.9/etc]

**Logs/Screenshots:**
[If applicable]
```

---

## Security & Compliance

This project implements comprehensive security controls aligned with **NEN-7510-2:2024**.

### Key Security Features

✅ **Separated Environments:**

- Test environment on `develop` branch
- Production environment on `main` branch
- Separate configuration and secrets per environment

✅ **Code Review & Approval Gates:**

- All code requires minimum 1 approval
- Branch protection prevents force pushes
- Audit trail for all changes

✅ **Security Scanning:**

- SAST (Static Application Security Testing) in CI/CD pipeline
- SCA (Software Composition Analysis) for dependencies
- Automated vulnerability detection

✅ **SBOM (Software Bill of Materials):**

- Generated per build (CycloneDX format)
- Required for NEN-7510 and CRA compliance

✅ **Logging & Monitoring:**

- Security events logged and audit-able
- Change tracking with full Git history

### Compliance Documentation

See [documentation/](documentation/) for:

- **Requirements.md** - Functional and non-functional requirements
- **dependency-review.md** - Dependency management guidelines
- **sbom.md** - Software Bill of Materials
- **environment-configuration.md** - Secure environment setup
- **permission-config.md** - Access control configuration

### Reporting Security Issues

⚠️ **Please do NOT create public GitHub issues for security vulnerabilities.**

Instead, email the security team with:

- Vulnerability description
- Affected code/component
- Reproduction steps
- Potential impact

The team will respond within 48 hours.

---

## Troubleshooting

### Build Issues

**Problem:** `mvn clean install` fails with dependency errors

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Re-run build with verbose output
mvn clean install -X

# Check for network issues
mvn dependency:resolve
```

**Problem:** Java version mismatch

```bash
# Check current Java version
java -version

# Set JAVA_HOME environment variable
export JAVA_HOME=/path/to/java8+  # Linux/Mac
# or in Windows: set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_XXX

# Verify Maven uses correct Java
mvn -v
```

### Test Failures

**Problem:** Tests fail locally but pass in CI

```bash
# Run tests with exact CI environment
mvn clean test -DargLine="-Xmx1024m"

# Check test dependencies
mvn dependency:tree

# Verify test database connectivity
# Check test-hibernate.cfg.xml in api/src/test/resources/
```

**Problem:** Database connectivity issues

```bash
# Verify test database is configured
cat api/src/test/resources/test-hibernate.cfg.xml

# Check MySQL connectivity
mysql -h localhost -u root -p

# Restart database service
sudo service mysql restart  # Linux
# or
mysql.server restart  # Mac
```

### IDE Issues

**IntelliJ IDEA:**

- File → Invalidate Caches → Restart
- Re-import Maven project: File → Open → Select pom.xml

**Eclipse:**

- Clean and rebuild: Project → Clean → Select Project → Build
- Update project: Right-click Project → Maven → Update Project

### Performance Issues

**Slow builds:**

```bash
# Use Maven daemon for faster builds
mvn -T 1C clean install  # Build with 1 thread per core

# Skip tests temporarily
mvn clean install -DskipTests

# Use offline mode (after initial download)
mvn -o clean install
```

---

## Resources

### Documentation

- [OpenMRS Documentation](https://wiki.openmrs.org/)
- [OpenMRS Appointment Module Wiki](https://wiki.openmrs.org/display/docs/Appointment+Module+Module)
- [OpenMRS Development Guide](https://wiki.openmrs.org/display/docs/OpenMRS+Module+Development)

### Project Documentation

- [Requirements](documentation/Requirements.md) - Functional and non-functional requirements
- [Environment Configuration](documentation/environment-configuration.md) - Setup and configuration
- [Branch Protection](documentation/branch-protection-documentation.md) - Git workflow rules
- [Code Review Process](documentation/review-documentation.md) - Review guidelines
- [Commit Conventions](documentation/commit-convention.md) - Git commit standards
- [Dependency Review](documentation/dependency-review.md) - Dependency management
- [SBOM Documentation](documentation/sbom.md) - Software Bill of Materials
- [Permission Configuration](documentation/permission-config.md) - Access control

### Tools & Technologies

- **Build Tool:** [Apache Maven](https://maven.apache.org/)
- **Framework:** [OpenMRS Platform](https://openmrs.org/)
- **ORM:** [Hibernate](https://hibernate.org/)
- **Testing:** [JUnit](https://junit.org/), [Mockito](https://site.mockito.org/)
- **Code Quality:** [SonarQube](https://www.sonarqube.org/)
- **Security Scanning:** [Grype (SCA)](https://github.com/anchore/grype)
- **Code Formatter:** OpenMRSFormatter.xml

### Community & Support

- **OpenMRS Talk Forum:** https://talk.openmrs.org/
- **GitHub Issues:** https://github.com/openmrs/openmrs-module-appointmentscheduling/issues
- **OpenMRS Slack:** [Join OpenMRS Slack](https://slack.openmrs.org/)

---

## License

This project is part of the OpenMRS Platform and follows the same licensing terms.
See [LICENSE.txt](openmrs-module-appointmentscheduling/LICENSE.txt) for details.

---

## Authors & Contributors

**Original Developers:** Tobin, Adam, Yonatan

**Current Maintainers:** OpenMRS Community

**Quality & Security Initiative:** Avans Hogeschool (LU2-Kwaliteit-Security-Verbetersonderzoek)

---

**Last Updated:** June 2026  
**Repository:** https://github.com/openmrs/openmrs-module-appointmentscheduling  
**Issues:** https://github.com/openmrs/openmrs-module-appointmentscheduling/issues

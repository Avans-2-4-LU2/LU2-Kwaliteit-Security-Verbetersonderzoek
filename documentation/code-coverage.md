# Code Coverage

## Objective

The objective of this task was to configure and activate code coverage measurement for the Appointment Scheduling Module and integrate coverage reporting into the CI/CD pipeline.

## Baseline Situation

At the start of the project, no code coverage tooling was configured.

As a result:

* Test coverage could not be measured.
* No coverage reports were generated.
* No coverage artifacts were available through the CI pipeline.
* Coverage metrics could not be used to evaluate software quality.

The baseline coverage report generated after configuring JaCoCo is shown below.

![Base Coverage Report](evidence/BaseCoverage.png)

## Implemented Improvement

JaCoCo was configured through the Maven build process using the `jacoco-maven-plugin`.

The plugin generates coverage reports during test execution and produces:

* HTML coverage reports
* XML coverage reports
* Coverage execution data

A dedicated GitHub Actions workflow was added to the CI/CD pipeline. The workflow automatically:

1. Executes the automated test suite.
2. Generates JaCoCo coverage reports.
3. Uploads the reports as GitHub Actions artifacts.

This ensures that coverage information is available for every relevant build and can be reviewed independently of a local development environment.

## Coverage Results

Coverage was measured using the following command:

```bash
mvn -pl api clean test
```

Results:

| Metric       | Coverage |
| ------------ | -------- |
| Instructions | 70%      |
| Branches     | 63%      |
| Lines        | 72%      |
| Methods      | 76%      |
| Classes      | 88%      |

## Coverage Target

A minimum line coverage target of 70% was selected.

### Rationale

The Appointment Scheduling Module is a legacy OpenMRS component containing a significant amount of existing functionality and technical debt. A 70% threshold provides a realistic balance between development effort and software quality while ensuring that most business logic is exercised by automated tests.

The measured line coverage of 72% exceeds the defined quality target.

## Coverage Interpretation and Risk-Based Testing

The measured line coverage of 72% demonstrates that a substantial portion of the codebase is executed during automated testing. However, code coverage alone is not considered sufficient evidence of software quality or security.

Coverage metrics indicate which code is executed by tests, but they do not demonstrate whether critical functionality, security controls, or high-risk scenarios have been adequately validated.

For this reason, coverage results were evaluated together with the CIA analysis performed for the Appointment Scheduling Module.

### Coverage and CIA Alignment

The CIA analysis identified several high-priority risks requiring mitigation before release, including:

* Unauthorized access to appointment records.
* Exposure of confidential appointment types.
* Incorrect scheduling information affecting appointment integrity.
* Missing or altered audit information.
* Insufficient privilege enforcement in the UI and API.

The existing automated test suite covers a significant portion of the appointment scheduling functionality, contributing to the measured 72% line coverage. In addition, the project team reviewed whether the implemented tests exercise the areas associated with the highest CIA risks.

Coverage is therefore used as a supporting quality metric rather than as a standalone objective.

### Limitations of Coverage Metrics

A high coverage percentage does not guarantee that critical functionality is tested correctly.

For example, it is possible to achieve a relatively high coverage percentage while still missing tests for security-critical functionality such as authorization checks, confidential appointment handling, or audit logging behaviour.

Therefore, coverage results should always be interpreted together with:

* Risk assessments.
* Security analyses.
* Functional test results.
* Code reviews.

The goal is not to maximize coverage at all costs, but to ensure that testing efforts focus on the most important business and security risks.

## Evidence

Coverage reports are generated in:

```text
api/target/site/jacoco
```

The CI pipeline publishes these reports as downloadable GitHub Actions artifacts, allowing reviewers to inspect coverage results without requiring a local build environment.

## Conclusion

Code coverage measurement has been successfully configured and integrated into the CI/CD process.

The project currently achieves 72% line coverage, exceeding the defined minimum target of 70%.

More importantly, testing efforts are evaluated in relation to the highest risks identified in the CIA analysis. This provides greater confidence than coverage metrics alone and supports the project's objective of improving software quality and security.

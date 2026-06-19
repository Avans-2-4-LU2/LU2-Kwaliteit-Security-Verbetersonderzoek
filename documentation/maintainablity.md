# Maintainability Improvement Report

# 1. Maintainability Analysis

## 1.1 Analysis Approach

To identify maintainability issues within the Appointment Scheduling Module, a combination of static analysis tools and source code metrics was used.

The maintainability assessment was based on the maintainability characteristics defined in ISO/IEC 25010:

* Modularity
* Reusability
* Analysability
* Modifiability
* Testability

The following tools were used during the analysis:

| Tool                            | Purpose                                                                                              |
| ------------------------------- | ---------------------------------------------------------------------------------------------------- |
| SonarCloud                      | Detection of code smells, duplication and maintainability issues                                     |
| JaCoCo                          | Verification that existing functionality remains covered by automated tests                          |
| IntelliJ IDEA + MetricsReloaded | Measurement of complexity metrics such as Cyclomatic Complexity and Weighted Methods per Class (WMC) |

MetricsReloaded is a third-party IntelliJ plugin used exclusively for local source code analysis. The tool operates on the developer workstation and does not transmit source code or project data to external services. It is therefore considered acceptable for maintainability analysis purposes and does not introduce additional risks to patient data confidentiality.

## 1.2 Initial Findings

Several static analysis techniques were evaluated to identify maintainability issues within the Appointment Scheduling Module.

SonarCloud was initially used to obtain a high-level overview of the project. However, the maintainability indicators provided by SonarCloud mainly consist of aggregated project-level metrics and ratings. While useful as a general quality overview, these metrics do not provide sufficient insight into the maintainability characteristics of individual classes or components.

In addition, SonarCloud primarily analysed resources located in the web module (`omod`) and provided limited information about the Java business logic contained in the `api` module.

Because the maintainability improvement effort focuses on the Java application layer, additional source code analysis was performed using MetricsReloaded. This allowed maintainability-related metrics to be collected directly from individual classes, including cyclomatic complexity and Weighted Methods per Class (WMC).

The collected metrics were subsequently used to identify maintainability hotspots and to support the selection of a suitable refactoring candidate.

## 1.3 Complexity Analysis

The class `AppointmentServiceImpl` was identified as a potential maintainability hotspot.

MetricsReloaded reported the following metrics:

| Metric                           | Value |
| -------------------------------- | ----- |
| Weighted Methods per Class (WMC) | 198   |
| Total Cyclomatic Complexity      | 207   |
| Average Cyclomatic Complexity    | 1.71  |
| Approximate Lines of Code        | 1500  |

A WMC value of 198 is considered exceptionally high and indicates that a large amount of functionality is concentrated within a single class.

Although most individual methods exhibit relatively low cyclomatic complexity, the class contains a very large number of methods and responsibilities. This increases the effort required to understand, modify, test and maintain the implementation.

## 1.4 Maintainability Hotspot Identification

Further inspection of `AppointmentServiceImpl` showed that the class contains responsibilities related to:

* Appointment management
* Appointment status handling
* Appointment confidentiality filtering
* Scheduling logic
* Provider schedule management
* Reporting and statistics
* Audit and logging functionality

This indicates a violation of the Single Responsibility Principle (SRP), as multiple distinct concerns are implemented within a single service class.

Based on the collected metrics and manual inspection, `AppointmentServiceImpl` was identified as the primary maintainability hotspot and selected for further improvement.

# 2. Improvement Selection

The maintainability analysis identified `AppointmentServiceImpl` as the primary maintainability hotspot.

Because refactoring the entire class was considered outside the scope of this project, a smaller and well-defined subset of functionality was selected.

A review of the responsibilities contained within `AppointmentServiceImpl` identified a group of methods responsible for handling appointment confidentiality:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

These methods implement a distinct responsibility related to confidentiality and access control.

The confidentiality functionality was selected for refactoring because:

* The methods represent a clearly identifiable concern.
* The functionality is closely related to previous CIA and security analyses performed during the project.
* The methods can be isolated without changing business behaviour.
* Existing automated tests provide protection against regressions.

Based on these considerations, the confidentiality filtering functionality, implemented through the methods:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

was selected as the maintainability improvement candidate.

The selected confidentiality filtering functionality is used by multiple public service operations, including:

- `getAllAppointments()`
- `getAllAppointments(boolean)`
- `getAppointmentsOfPatient()`
- `getAppointmentsByConstraints()`
- `getScheduledAppointmentsForPatient()`

This confirms that the functionality is actively used throughout the module and therefore represents a suitable candidate for maintainability improvement.

# 3. Test Baseline

Before implementing any maintainability improvements, the existing automated test coverage was reviewed.

The objective of this review was to determine whether the selected confidentiality filtering functionality was already protected by automated tests. A sufficient test baseline reduces the risk of introducing regressions during refactoring and provides confidence that behaviour remains unchanged after the implementation.

## 3.1 Tested Functionality

The selected maintainability improvement focuses on the confidentiality filtering functionality implemented through the following methods:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

These methods are responsible for restricting access to confidential appointment information based on user privileges.

Because these methods are internal helper methods, they are not tested directly. Instead, they are exercised through public service operations that invoke the confidentiality filtering logic.

Examples include:

* `getAllAppointments()`
* `getAppointment()`
* `getAppointmentsByConstraints()`
* `getScheduledAppointmentsForPatient()`
* `getLastAppointment()`

## 3.2 Existing Test Coverage

A review of the existing test suite identified several tests that exercise the confidentiality filtering functionality through the public service layer.

The most relevant test class is:

* `ConfidentialAppointmentAccessControlTest`

This class contains dedicated tests that verify confidentiality behaviour for both authorized and unauthorized users.

Relevant test cases include:

* `getAppointment_filtersConfidentialAppointmentForUnauthorizedUser()`
* `getAppointmentsByConstraints_omitsConfidentialAppointmentsForUnauthorizedUser()`
* `getScheduledAppointmentsForPatient_omitsConfidentialAppointmentsForUnauthorizedUser()`
* `getLastAppointment_filtersConfidentialAppointmentForUnauthorizedUser()`
* `confidentialAppointment_consistentlyFilteredAcrossReadPaths()`

These tests validate that confidential appointments remain inaccessible to users who do not possess the required privileges.

Additional regression protection is provided by existing service-level tests within `AppointmentServiceTest`, which verify the correctness of appointment retrieval functionality.

## 3.3 Baseline Verification

Prior to refactoring, the complete automated test suite was executed using:

```bash
mvn -pl api clean test
```

Results:

| Metric         | Result |
| -------------- |--------|
| Tests Executed | 198    |
| Failures       | 0      |
| Errors         | 0      |
| Skipped        | 0      |

The successful execution of the test suite establishes a stable baseline against which the refactored implementation can later be validated.

## 3.4 Regression Protection

The identified confidentiality tests provide regression protection for the selected maintainability improvement.

Because the refactoring targets the internal implementation of the confidentiality filtering functionality rather than its external behaviour, these tests are expected to continue passing after the refactoring has been completed.

Following implementation, the same test suite will be executed again to verify that:

* Confidential appointments remain inaccessible to unauthorized users.
* Appointment retrieval behaviour remains unchanged.
* No functional regressions have been introduced.
* The maintainability improvements preserve existing functionality.

# 4. Refactoring Design

## 4.1 Current Design

The maintainability analysis identified that `AppointmentServiceImpl` contains multiple unrelated responsibilities.

One of these responsibilities is confidentiality filtering, implemented through:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

These methods are embedded within a large service class that already contains appointment management, appointment status handling, scheduling logic, provider schedule management, reporting functionality and audit-related functionality.

As a result, confidentiality-related logic is mixed with other concerns, making the class more difficult to understand, maintain and modify.

## 4.2 Applied Design Principles

The refactoring is primarily based on the following maintainability principles.

### Single Responsibility Principle (SRP)

According to the Single Responsibility Principle, a class should have only one reason to change.

`AppointmentServiceImpl` currently contains multiple responsibilities. Confidentiality filtering represents a distinct concern that can evolve independently from appointment scheduling, appointment management or reporting functionality.

Separating this concern improves modularity and reduces coupling.

### Separation of Concerns

The confidentiality filtering functionality is logically independent from appointment retrieval and scheduling operations.

Extracting the confidentiality logic into a dedicated component improves readability and makes future modifications easier to implement.

## 4.3 Refactoring Strategy

The selected refactoring strategy is **Extract Class**.

A new class named:

```java
AppointmentConfidentialityService
```

will be introduced.

This class will become responsible for:

* Determining whether an appointment is confidential.
* Filtering confidential appointments for unauthorized users.
* Applying confidentiality checks to collections of appointments.

The following methods will be moved from `AppointmentServiceImpl` into the new class:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

## 4.4 Proposed Design

### Current Structure

```text
AppointmentServiceImpl
├ Appointment management
├ Appointment status handling
├ Scheduling logic
├ Provider schedule management
├ Reporting and statistics
├ Confidentiality filtering
└ Audit and logging functionality
```

### Proposed Structure

```text
AppointmentServiceImpl
├ Appointment management
├ Appointment status handling
├ Scheduling logic
├ Provider schedule management
├ Reporting and statistics
├ Audit and logging functionality
└ Uses AppointmentConfidentialityService

AppointmentConfidentialityService
├ isConfidentialAppointment()
├ filterConfidentialAppointmentIfNotAuthorized()
└ removeConfidentialAppointmentsIfNotAuthorized()
```

In the proposed design, `AppointmentServiceImpl` remains responsible for appointment-related business operations, while confidentiality-related decisions are delegated to `AppointmentConfidentialityService`.

This reduces the number of responsibilities contained within `AppointmentServiceImpl` and improves separation of concerns.

## 4.5 Expected Benefits

The proposed design is expected to improve maintainability by:

* Reducing the size of `AppointmentServiceImpl`.
* Improving modularity.
* Improving readability.
* Improving analysability.
* Improving testability of confidentiality-related functionality.
* Reducing the number of responsibilities contained within a single class.

These improvements directly support the maintainability characteristics defined by ISO 25010.

# 5. Implementation

## 5.1 Created Components

To implement the proposed refactoring, a new class named `AppointmentConfidentialityService` was introduced.

The purpose of this class is to encapsulate all confidentiality-related business logic that was previously implemented inside `AppointmentServiceImpl`.

This separates confidentiality filtering from the broader appointment management responsibilities and improves the overall modularity of the implementation.

## 5.2 Migrated Functionality

The following methods were moved from `AppointmentServiceImpl` to `AppointmentConfidentialityService`:

* `removeConfidentialAppointmentsIfNotAuthorized()`
* `filterConfidentialAppointmentIfNotAuthorized()`
* `isConfidentialAppointment()`

No functional changes were made to the implementation logic of these methods. The refactoring focused solely on improving the structure and maintainability of the codebase.

## 5.3 Service Integration

After extracting the confidentiality functionality, `AppointmentServiceImpl` was updated to use the new service.

Previously, confidentiality checks were performed directly within the class itself.

After refactoring, confidentiality-related decisions are delegated to `AppointmentConfidentialityService`.

This allows `AppointmentServiceImpl` to focus on appointment-related business operations while confidentiality concerns are handled by a dedicated component.

## 6. Validation

### 6.1 Complexity Validation

After implementing the refactoring, the maintainability metrics were collected again using MetricsReloaded.

#### AppointmentServiceImpl

| Metric                           | Before | After |
| -------------------------------- | ------ | ----- |
| Weighted Methods per Class (WMC) | 198    | 193   |
| Total Cyclomatic Complexity      | 207    | 198   |
| Average Cyclomatic Complexity    | 1.71   | 1.65  |

The reduction in complexity is the result of extracting the confidentiality-related functionality from `AppointmentServiceImpl` into a dedicated service.

Although the overall functionality of the system remained unchanged, the complexity is now distributed across multiple classes with clearer responsibilities. This improves analysability and modifiability and reduces the amount of functionality concentrated in a single class.

### 6.2 Extracted Service Metrics

The newly introduced `AppointmentConfidentialityServiceImpl` was analysed separately.

| Metric                           | Value |
| -------------------------------- | ----- |
| Weighted Methods per Class (WMC) | 7     |
| Average Cyclomatic Complexity    | 2.33  |
| Maximum Cyclomatic Complexity    | 4     |
| Number of Methods                | 3     |

The extracted service contains only confidentiality-related functionality and exhibits low complexity. This indicates that the responsibility has been isolated into a cohesive and maintainable component.

### 6.3 Regression Testing

After the refactoring was completed, the automated test suite was executed again.

| Metric         | Result |
| -------------- | ------ |
| Tests Executed | 198    |
| Failures       | 0      |
| Errors         | 0      |
| Skipped        | 0      |

All tests completed successfully.

The successful execution of the test suite demonstrates that the refactoring did not introduce regressions and that the functional behaviour of the Appointment Scheduling Module remained unchanged.

### 6.4 Maintainability Evaluation

The refactoring improved the maintainability of the Appointment Scheduling Module by separating confidentiality filtering from the general appointment management logic.

The resulting design provides:

* Improved modularity through the introduction of a dedicated confidentiality service.
* Improved analysability by reducing the size and responsibilities of `AppointmentServiceImpl`.
* Improved modifiability because confidentiality rules are now located in a single component.
* Better adherence to the Single Responsibility Principle (SRP).

The maintainability improvements were achieved without affecting the existing functionality, as confirmed by the successful execution of the automated test suite.

# 7. Conclusion

The objective of this maintainability improvement was to identify a maintainability hotspot within the Appointment Scheduling Module and implement a maintainability-focused improvement without affecting existing functionality.

The maintainability analysis identified `AppointmentServiceImpl` as the primary hotspot. MetricsReloaded reported a Weighted Methods per Class (WMC) value of 198 and a total cyclomatic complexity of 207, indicating that a large amount of functionality was concentrated within a single class.

A confidentiality-related responsibility was selected for improvement. Using an Extract Class refactoring, the confidentiality filtering functionality was moved to a dedicated `AppointmentConfidentialityService`.

After the refactoring, the complexity metrics of `AppointmentServiceImpl` decreased from:

* WMC: 198 → 193
* Total Cyclomatic Complexity: 207 → 198

In addition, the extracted service exhibited a low complexity level and a clearly defined responsibility.

Regression testing showed that all 198 automated tests continued to pass successfully, demonstrating that the maintainability improvements did not introduce functional regressions.

The refactoring improved modularity, analysability and modifiability while increasing adherence to the Single Responsibility Principle. The maintainability objective was therefore achieved successfully.

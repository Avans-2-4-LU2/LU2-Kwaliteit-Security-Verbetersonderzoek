# Build Issues

## Invalid Method Reference in AppointmentServiceImpl

### Description

During the setup of code coverage tooling, the project was migrated to a Java 8 build environment to match the configuration used in the CI pipeline. After resolving the initial Java version compatibility issues, the build failed due to a compilation error in `AppointmentServiceImpl.java`.

### Error

```text
cannot find symbol
method getAppointmentsForPatient(Patient)
```

### Root Cause

The method `getAppointmentsForPatient(Patient)` was referenced inside the method `getAppointmentsForPatientWithLogging(Patient)`, but no such method exists in the project.

The implementation already contains a valid method named:

```java
getAppointmentsOfPatient(Patient)
```

As a result, the Java compiler could not resolve the method reference and the build failed during compilation.

### Resolution

The invalid method call was replaced with the existing implementation:

**Before**

```java
return getAppointmentsForPatient(patient);
```

**After**

```java
return getAppointmentsOfPatient(patient);
```

### Verification

The project was rebuilt after applying the fix.

Results:

* The API module compiled successfully.
* All unit tests completed successfully.
* Test execution summary:

```text
Tests run: 167
Failures: 0
Errors: 0
Skipped: 0
```

### Impact

This change does not modify business functionality or security controls.

The fix only restores successful compilation of the code and enables further work on code coverage analysis and CI/CD quality tooling.




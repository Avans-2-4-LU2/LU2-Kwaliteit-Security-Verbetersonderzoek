package org.openmrs.module.appointmentscheduling.api;

import org.junit.Assert;
import org.junit.Test;

public class SecurityLoggerTest {

    @Test
    public void maskSensitiveData_shouldMaskBsn() {
        String input = "Patient BSN is 123456789.";
        String result = SecurityLogger.maskSensitiveData(input);
        Assert.assertEquals("Patient BSN is ***.", result);
    }

    @Test
    public void maskSensitiveData_shouldMaskPassword() {
        String input = "Login failed for user with password=mySecretPassword&user=admin";
        String result = SecurityLogger.maskSensitiveData(input);
        Assert.assertEquals("Login failed for user with password=***&user=admin", result);
        
        String input2 = "password:mySecretPassword";
        String result2 = SecurityLogger.maskSensitiveData(input2);
        Assert.assertEquals("password=***", result2); // Note: regex replaces the whole match with $1=*** so 'password:mySecretPassword' -> 'password=***'
    }

    @Test
    public void maskSensitiveData_shouldMaskToken() {
        String input = "Using token=abc123xyz for session";
        String result = SecurityLogger.maskSensitiveData(input);
        Assert.assertEquals("Using token=*** for session", result);
    }

    @Test
    public void maskSensitiveData_shouldNotMaskNormalText() {
        String input = "This is a normal log message with numbers 12345 and normal words.";
        String result = SecurityLogger.maskSensitiveData(input);
        Assert.assertEquals(input, result);
    }

    @Test
    public void logSuccessfulAction_shouldLogWithoutException() {
        // Just invoking it to ensure no exceptions are thrown, and verifying it accepts parameters correctly.
        SecurityLogger.logSuccessfulAction("LOGIN", "User admin logged in successfully.");
    }

    @Test
    public void logFailedAction_shouldLogWithoutException() {
        SecurityLogger.logFailedAction("LOGIN", "Failed login for admin with password=secret");
    }
}

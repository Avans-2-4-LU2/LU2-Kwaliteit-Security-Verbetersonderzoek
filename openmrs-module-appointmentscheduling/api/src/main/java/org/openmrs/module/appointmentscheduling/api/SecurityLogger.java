package org.openmrs.module.appointmentscheduling.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SecurityLogger {

    private static final Log log = LogFactory.getLog(SecurityLogger.class);

    public static String maskSensitiveData(String message) {
        if (message == null) {
            return null;
        }
        String masked = message;
        // Mask 9-digit BSN/SSN
        masked = masked.replaceAll("\\b\\d{9}\\b", "***");
        
        // Mask passwords: password=secret -> password=***
        masked = masked.replaceAll("(?i)(password|pwd)\\s*[=:]\\s*([^\\s&]+)", "$1=***");
        
        // Mask tokens
        masked = masked.replaceAll("(?i)(token|sessionToken)\\s*[=:]\\s*([^\\s&]+)", "$1=***");
        
        return masked;
    }

    public static void logSuccessfulAction(String action, String details) {
        log.info("SECURITY SUCCESS - Action: " + action + " | Details: " + maskSensitiveData(details));
    }

    public static void logFailedAction(String action, String details) {
        log.warn("SECURITY FAILED - Action: " + action + " | Details: " + maskSensitiveData(details));
    }
}

package org.openmrs.module.appointmentscheduling.api;

import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class SecurityLoggerLogOutputTest {

    private static class TestLog implements Log {
        private final List<String> messages = new ArrayList<String>();

        private void capture(Object message) {
            messages.add(message == null ? "" : message.toString());
        }

        public List<String> getMessages() {
            return messages;
        }

        public void debug(Object message) {
            capture(message);
        }

        public void debug(Object message, Throwable t) {
            capture(message);
        }

        public void error(Object message) {
            capture(message);
        }

        public void error(Object message, Throwable t) {
            capture(message);
        }

        public void fatal(Object message) {
            capture(message);
        }

        public void fatal(Object message, Throwable t) {
            capture(message);
        }

        public void info(Object message) {
            capture(message);
        }

        public void info(Object message, Throwable t) {
            capture(message);
        }

        public void trace(Object message) {
            capture(message);
        }

        public void trace(Object message, Throwable t) {
            capture(message);
        }

        public void warn(Object message) {
            capture(message);
        }

        public void warn(Object message, Throwable t) {
            capture(message);
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public boolean isFatalEnabled() {
            return true;
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public boolean isTraceEnabled() {
            return true;
        }

        public boolean isWarnEnabled() {
            return true;
        }
    }

    @Test
    public void loggedMessages_shouldNotContainPlaintextSensitiveData() throws Exception {
        Field logField = SecurityLogger.class.getDeclaredField("log");
        logField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(logField, logField.getModifiers() & ~Modifier.FINAL);

        Log originalLog = (Log) logField.get(null);
        TestLog testLog = new TestLog();

        try {
            logField.set(null, testLog);

            String failedDetails = "Failed login for admin with password=secret and token=abc123";
            String successDetails = "User admin logged in with token=xyz789 and password=myPwd";

            SecurityLogger.logFailedAction("LOGIN", failedDetails);
            SecurityLogger.logSuccessfulAction("LOGIN", successDetails);

            List<String> logged = testLog.getMessages();
            Assert.assertEquals("Expected exactly 2 log entries", 2, logged.size());

            for (String msg : logged) {
                Assert.assertFalse("Log contains raw password", msg.contains("secret"));
                Assert.assertFalse("Log contains raw token", msg.contains("abc123"));
                Assert.assertFalse("Log contains raw token", msg.contains("xyz789"));
                Assert.assertFalse("Log contains raw password", msg.contains("myPwd"));

                Assert.assertTrue("Password masking not found",
                        msg.contains("password=***") || msg.contains("pwd=***") || msg.contains("password:***"));
                Assert.assertTrue("Token masking not found",
                        msg.contains("token=***") || msg.contains("sessionToken=***"));
            }
        } finally {
            logField.set(null, originalLog);
            modifiersField.setInt(logField, logField.getModifiers() | Modifier.FINAL);
        }
    }
}

package org.openmrs.module.appointmentscheduling.security;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentSchedulingConstants;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Penetration-test demonstration for issue #36 (test case PT-01).
 *
 * FINDING - broken access control (CWE-285 / CWE-639):
 * The confidential-appointment privilege
 * ({@link AppointmentSchedulingConstants#PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS},
 * "Task: appointmentschedulingui.viewConfidential") is only enforced in the
 * reporting data evaluators (PatientToAppointmentDataEvaluator /
 * PersonToAppointmentDataEvaluator). The core AppointmentService retrieval
 * methods are gated only by "View Appointments" and do NOT filter confidential
 * appointments. A user who may view appointments but lacks the confidentiality
 * privilege can therefore read confidential appointment data.
 *
 * Attacker: the standard OpenMRS test user "butch", who does NOT hold the
 * confidentiality privilege. (The same user the reporting test
 * PatientToAppointmentDataEvaluatorTest uses to prove that the reporting path
 * DOES filter confidential appointments - that test is the PT-02 control.) We
 * grant only "View Appointments" via a proxy privilege so this test isolates the
 * confidentiality gap rather than a generic authorization failure.
 *
 * Confidential test data (standardAppointmentTestDataset.xml):
 * appointment_type_id=1 ("Initial HIV Clinic Appointment", confidential=1);
 * appointment id 1 is of that type.
 *
 * NOTE ON SPRINT SCOPE (#36 is sprint 2 = demonstrate + document):
 * This test asserts the CURRENT, insecure behaviour so that it passes and serves
 * as documented evidence of the leak. In sprint 3, after the missing
 * confidentiality check is added to the core retrieval paths, these expectations
 * must be inverted to assert that the confidential appointment (id 1) is filtered
 * out - the red/green re-test that proves the risk is reduced.
 */
public class ConfidentialAppointmentAccessControlTest extends BaseModuleContextSensitiveTest {

    private static final int CONFIDENTIAL_APPOINTMENT_ID = 1;

    @Before
    public void setup() throws Exception {
        executeDataSet("standardAppointmentTestDataset.xml");
    }

    /**
     * Become "butch" (no confidentiality privilege) and grant only the legitimate
     * "View Appointments" / "View Appointment Types" privileges.
     */
    private AppointmentService becomeAttacker() {
        Context.becomeUser("butch");

        // Precondition: the attacker must NOT hold the confidentiality privilege,
        // otherwise this would not demonstrate the gap.
        assertFalse("Test precondition: attacker must lack the confidentiality privilege",
                Context.hasPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS));

        // The attacker is legitimately allowed to view appointments.
        Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
        Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);

        return Context.getService(AppointmentService.class);
    }

    private void resetAttacker() {
        Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
        Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);
    }

    /**
     * PT-01a: single-record retrieval leaks a confidential appointment.
     */
    @Test
    @DirtiesContext
    public void getAppointment_currentlyLeaksConfidentialAppointment_FINDING() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            Appointment confidential = service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID);

            assertTrue("Sanity: appointment 1 should be of a confidential type",
                    confidential != null && confidential.getAppointmentType().isConfidential());

            // FINDING (PT-01): the confidential appointment is returned in full to a
            // user without the confidentiality privilege. A secure implementation
            // would deny or filter this.
            assertNotNull("FINDING (PT-01): confidential appointment leaked to a user "
                    + "without the confidentiality privilege", confidential);
        } finally {
            resetAttacker();
        }
    }

    /**
     * PT-01b: list retrieval leaks confidential appointments.
     */
    @Test
    @DirtiesContext
    public void getAllAppointments_currentlyLeaksConfidentialAppointments_FINDING() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            List<Appointment> appointments = service.getAllAppointments();

            boolean confidentialLeaked = false;
            for (Appointment a : appointments) {
                if (a.getAppointmentType() != null && a.getAppointmentType().isConfidential()) {
                    confidentialLeaked = true;
                    break;
                }
            }

            // FINDING (PT-01): the list retrieval path also returns confidential
            // appointments to a user without the confidentiality privilege.
            assertTrue("FINDING (PT-01): getAllAppointments leaked confidential appointment(s) "
                    + "to a user without the confidentiality privilege", confidentialLeaked);
        } finally {
            resetAttacker();
        }
    }
}

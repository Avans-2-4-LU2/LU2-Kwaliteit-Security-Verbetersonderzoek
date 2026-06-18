package org.openmrs.module.appointmentscheduling.security;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Visit;
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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Penetration-test re-test for the confidential-appointment access-control finding
 * (F-01, issue #36; mitigation issue #71 / SR-03).
 *
 * FINDING (sprint 2): the confidentiality privilege
 * ({@link AppointmentSchedulingConstants#PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS},
 * "Task: appointmentschedulingui.viewConfidential") was only enforced in the reporting data evaluators
 * (PatientToAppointmentDataEvaluator / PersonToAppointmentDataEvaluator). The core AppointmentService
 * retrieval methods were gated only by "View Appointments" and returned confidential appointments to any
 * user with that privilege.
 *
 * MITIGATION (sprint 3): AppointmentServiceImpl now filters confidential appointments for users without
 * the confidentiality privilege - single-record retrieval returns null, list retrieval omits them.
 *
 * This test asserts the SECURE behaviour: a low-privilege attacker ("butch") cannot read confidential
 * appointments, while a user granted the confidentiality privilege still can (proving the fix does not
 * over-filter). Before the mitigation these assertions fail (the appointment leaks); after it they pass -
 * the red/green proof that the risk is reduced.
 *
 * Confidential test data (standardAppointmentTestDataset.xml): appointment_type_id=1
 * ("Initial HIV Clinic Appointment", confidential=1); appointment id 1 is of that type.
 */
public class ConfidentialAppointmentAccessControlTest extends BaseModuleContextSensitiveTest {

    private static final int CONFIDENTIAL_APPOINTMENT_ID = 1;
    private static final String CONFIDENTIAL_APPOINTMENT_UUID = "c0c579b0-8e59-401d-8a4a-976a0b183601";

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

        // Precondition: the attacker must NOT hold the confidentiality privilege.
        assertFalse("Test precondition: attacker must lack the confidentiality privilege",
                Context.hasPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS));

        Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
        Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);

        return Context.getService(AppointmentService.class);
    }

    private void resetAttacker() {
        Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
        Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);
    }

    private boolean containsConfidential(List<Appointment> appointments) {
        for (Appointment a : appointments) {
            if (a.getAppointmentType() != null && a.getAppointmentType().isConfidential()) {
                return true;
            }
        }
        return false;
    }

    /**
     * PT-01a (re-test): single-record retrieval filters the confidential appointment for a user without
     * the privilege, but still returns it once the privilege is granted (no over-filtering).
     */
    @Test
    @DirtiesContext
    public void getAppointment_filtersConfidentialAppointmentForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            // MITIGATED: confidential appointment is no longer returned without the privilege.
            assertNull("Confidential appointment must not be returned to a user without the "
                    + "confidentiality privilege", service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID));

            // No over-filtering: with the privilege the appointment is visible and is confidential.
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment visible = service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
            assertNotNull("Authorized user should still see the confidential appointment", visible);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    visible.getAppointmentType().isConfidential());
            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
        } finally {
            resetAttacker();
        }
    }

    /**
     * PT-01b (re-test): list retrieval omits confidential appointments for a user without the privilege,
     * but includes them once the privilege is granted (no over-filtering).
     */
    @Test
    @DirtiesContext
    public void getAllAppointments_omitsConfidentialAppointmentsForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            // MITIGATED: no confidential appointment appears in the list without the privilege.
            assertFalse("List retrieval must not expose confidential appointments to a user without the "
                    + "confidentiality privilege", containsConfidential(service.getAllAppointments()));

            // No over-filtering: with the privilege, confidential appointments are present.
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            assertTrue("Authorized user should see confidential appointments in the list",
                    containsConfidential(service.getAllAppointments()));
            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
        } finally {
            resetAttacker();
        }
    }

    @Test
    @DirtiesContext
    public void getAppointmentByUuid_filtersConfidentialAppointmentForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment visible = service.getAppointmentByUuid(CONFIDENTIAL_APPOINTMENT_UUID);
            assertNotNull("Authorized user should still see the confidential appointment by UUID", visible);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    visible.getAppointmentType().isConfidential());
            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);

            assertNull("Confidential appointment must not be returned by UUID to a user without the "
                    + "confidentiality privilege",
                    service.getAppointmentByUuid(CONFIDENTIAL_APPOINTMENT_UUID));
        } finally {
            resetAttacker();
        }
    }

    @Test
    @DirtiesContext
    public void getAppointmentByVisit_filtersConfidentialAppointmentForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment visible = service.getAppointmentByVisit(new Visit(1));
            assertNotNull("Authorized user should still see the confidential appointment", visible);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    visible.getAppointmentType().isConfidential());
            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);

            assertNull("Confidential appointment must not be returned by visit to a user without the "
                    + "confidentiality privilege", service.getAppointmentByVisit(new Visit(1)));
        } finally {
            resetAttacker();
        }
    }

    @Test
    @DirtiesContext
    public void getAppointmentsByConstraints_omitsConfidentialAppointmentsForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment visible = service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
            assertNotNull("Authorized user should still see the confidential appointment", visible);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    visible.getAppointmentType().isConfidential());

            List<Appointment> authorized = service.getAppointmentsByConstraints(null, null, null, null,
                    visible.getAppointmentType(), null);
            assertTrue("Authorized user should see at least one confidential appointment in the constraints result",
                    containsConfidential(authorized));

            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            List<Appointment> unauthorized = service.getAppointmentsByConstraints(null, null, null, null,
                    visible.getAppointmentType(), null);
            assertFalse("Confidential appointments must not be returned by constraints to a user without the "
                    + "confidentiality privilege", containsConfidential(unauthorized));
        } finally {
            resetAttacker();
        }
    }

    @Test
    @DirtiesContext
    public void getScheduledAppointmentsForPatient_omitsConfidentialAppointmentsForUnauthorizedUser() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment visible = service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
            assertNotNull("Authorized user should still see the confidential appointment", visible);
            Patient patient = visible.getPatient();
            assertNotNull("Sanity: confidential appointment should have a patient", patient);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    visible.getAppointmentType().isConfidential());

            List<Appointment> authorized = service.getScheduledAppointmentsForPatient(patient);
            assertTrue("Authorized user should see the confidential appointment in the scheduled list",
                    containsConfidential(authorized));

            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            List<Appointment> unauthorized = service.getScheduledAppointmentsForPatient(patient);
            assertFalse("Confidential appointments must not be returned in the scheduled list to a user without the "
                    + "confidentiality privilege", containsConfidential(unauthorized));
        } finally {
            resetAttacker();
        }
    }

    /**
     * Cross-entry-point / cross-component consistency (tasks 4 and 5): the same confidential appointment
     * is filtered for an unauthorized user across every user-facing read path (single, list, by-patient),
     * and visible to an authorized user. The REST resources and UI controllers read through these same
     * service methods, so the service-layer choke point covers them.
     */
    @Test
    @DirtiesContext
    public void confidentialAppointment_consistentlyFilteredAcrossReadPaths() throws Exception {
        AppointmentService service = becomeAttacker();
        try {
            // Establish the patient and confidentiality as an authorized user.
            Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
            Appointment confidential = service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
            assertNotNull("Authorized user should see the confidential appointment", confidential);
            assertTrue("Sanity: appointment 1 is of a confidential type",
                    confidential.getAppointmentType().isConfidential());
            Patient patient = confidential.getPatient();
            assertTrue("Authorized user sees it via the by-patient path",
                    containsConfidential(service.getAppointmentsOfPatient(patient)));
            Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);

            // Unauthorized user: filtered consistently across every read path.
            assertNull("single retrieval must filter the confidential appointment",
                    service.getAppointment(CONFIDENTIAL_APPOINTMENT_ID));
            assertFalse("list retrieval must filter the confidential appointment",
                    containsConfidential(service.getAllAppointments()));
            assertFalse("by-patient retrieval must filter the confidential appointment",
                    containsConfidential(service.getAppointmentsOfPatient(patient)));
        } finally {
            resetAttacker();
        }
    }
}

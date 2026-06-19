package org.openmrs.module.appointmentscheduling.security;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentSchedulingConstants;
import org.openmrs.module.appointmentscheduling.AppointmentUtils;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.module.appointmentscheduling.web.AppointmentData;
import org.openmrs.module.appointmentscheduling.web.DWRAppointmentService;
import org.openmrs.module.appointmentscheduling.web.controller.PatientDashboardAppointmentExtController;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for the entry points the original SR-03 filter pass missed: getLastAppointment(Patient)
 * read the DAO directly with no confidentiality check, and two callers (DWRAppointmentService,
 * PatientDashboardAppointmentExtController) dereferenced its result - and AppointmentService#getAppointment's
 * result - without a null check. Once those service methods were filtered, those callers would either NPE, or
 * - in the dashboard controller's case - silently modify a confidential appointment without ever checking the
 * privilege at all.
 *
 * Fixture (standardWebAppointmentTestDataset.xml): patient 2's most recent appointment by time-slot start date
 * is appointment 2, of the confidential type (appointment_type_id=1).
 */
public class ConfidentialAppointmentDashboardRegressionTest extends BaseModuleWebContextSensitiveTest {

	private static final int CONFIDENTIAL_PATIENT_ID = 2;

	private static final int CONFIDENTIAL_APPOINTMENT_ID = 2;

	private static final int CONFIDENTIAL_APPOINTMENT_BLOCK_ID = 1;

	private static final String CONFIDENTIAL_APPOINTMENT_TYPE_NAME = "Initial HIV Clinic Appointment";

	@Before
	public void setup() throws Exception {
		executeDataSet("standardWebAppointmentTestDataset.xml");
		// getPatientDescription() parses this global property as an int; it is normally set via the
		// module's settings form and isn't part of the standard test data, so without it any call would
		// NumberFormatException regardless of authorization.
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(AppointmentUtils.GP_PATIENT_PHONE_NUMBER, "1"));
	}

	private void becomeAttacker() {
		Context.becomeUser("butch");
		assertFalse("Test precondition: attacker must lack the confidentiality privilege",
		    Context.hasPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS));
		// Baseline privileges a clinical user navigating the patient dashboard legitimately has -
		// PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS is deliberately withheld; that's what is under test.
		Context.addProxyPrivilege(PrivilegeConstants.VIEW_PATIENTS);
		Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);
		Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);
	}

	private void resetAttacker() {
		Context.removeProxyPrivilege(PrivilegeConstants.VIEW_PATIENTS);
		Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_TYPES);
		Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENT_BLOCKS);
	}

	@Test
	@DirtiesContext
	public void checkProviderOpenConsultations_doesNotThrowOrLeakForConfidentialAppointment() throws Exception {
		becomeAttacker();
		try {
			Boolean result = new DWRAppointmentService().checkProviderOpenConsultations(CONFIDENTIAL_APPOINTMENT_ID);
			assertFalse("A confidential appointment filtered to null must not be reported as an open consultation",
			    result);
		} finally {
			resetAttacker();
		}
	}

	@Test
	@DirtiesContext
	public void checkProviderOpenConsultationsByPatient_doesNotThrowOrLeakForConfidentialAppointment() throws Exception {
		becomeAttacker();
		try {
			Boolean result = new DWRAppointmentService()
			        .checkProviderOpenConsultationsByPatient(CONFIDENTIAL_PATIENT_ID);
			assertFalse(
			    "A confidential last appointment filtered to null must not be reported as an open consultation",
			    result);
		} finally {
			resetAttacker();
		}
	}

	@Test
	@DirtiesContext
	public void dashboardController_doesNotModifyConfidentialAppointmentForUnauthorizedUser() throws Exception {
		becomeAttacker();
		try {
			new PatientDashboardAppointmentExtController().showForm(null, CONFIDENTIAL_PATIENT_ID, "startConsult");
		} finally {
			resetAttacker();
		}

		Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
		Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		try {
			Appointment appointment = Context.getService(AppointmentService.class)
			        .getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
			assertNotNull(appointment);
			assertEquals("Unauthorized startConsult must not change the confidential appointment's status",
			    Appointment.AppointmentStatus.MISSED, appointment.getStatus());
		} finally {
			Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
			Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		}
	}

	@Test
	@DirtiesContext
	public void dashboardController_stillModifiesConfidentialAppointmentForAuthorizedUser() throws Exception {
		Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
		Context.addProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		try {
			new PatientDashboardAppointmentExtController().showForm(null, CONFIDENTIAL_PATIENT_ID, "startConsult");

			Appointment appointment = Context.getService(AppointmentService.class)
			        .getAppointment(CONFIDENTIAL_APPOINTMENT_ID);
			assertNotNull(appointment);
			assertEquals("Authorized startConsult should still update the confidential appointment's status",
			    Appointment.AppointmentStatus.INCONSULTATION, appointment.getStatus());
		} finally {
			Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
			Context.removeProxyPrivilege(AppointmentUtils.PRIV_VIEW_APPOINTMENTS);
		}
	}

	private boolean anyAppointmentOfConfidentialType(List<List<AppointmentData>> patientsInBlock) {
		for (List<AppointmentData> bucket : patientsInBlock) {
			for (AppointmentData data : bucket) {
				if (CONFIDENTIAL_APPOINTMENT_TYPE_NAME.equals(data.getAppointmentType())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * getAppointmentsInTimeSlot is intentionally unfiltered (occupancy/conflict logic needs to see every
	 * appointment), but getPatientsInAppointmentBlock uses it to build display data for
	 * appointmentBlockList.jsp / appointmentBlockForm.jsp (wired via config.xml's DWR include) - a real UI
	 * entry point that was missed by the original SR-03 filter pass and leaked full confidential appointment
	 * content (patient identity, appointment type name, reason) with no privilege check at all.
	 */
	@Test
	@DirtiesContext
	public void getPatientsInAppointmentBlock_doesNotLeakConfidentialAppointmentContentForUnauthorizedUser()
	        throws Exception {
		becomeAttacker();
		try {
			List<List<AppointmentData>> patientsInBlock = new DWRAppointmentService()
			        .getPatientsInAppointmentBlock(CONFIDENTIAL_APPOINTMENT_BLOCK_ID);
			assertFalse("Confidential appointment content must not be displayed to a user without the "
			        + "confidentiality privilege", anyAppointmentOfConfidentialType(patientsInBlock));
		} finally {
			resetAttacker();
		}
	}

	@Test
	@DirtiesContext
	public void getPatientsInAppointmentBlock_stillShowsConfidentialAppointmentContentForAuthorizedUser()
	        throws Exception {
		Context.addProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
		try {
			List<List<AppointmentData>> patientsInBlock = new DWRAppointmentService()
			        .getPatientsInAppointmentBlock(CONFIDENTIAL_APPOINTMENT_BLOCK_ID);
			assertTrue("Authorized user should still see confidential appointment content",
			    anyAppointmentOfConfidentialType(patientsInBlock));
		} finally {
			Context.removeProxyPrivilege(AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
		}
	}
}

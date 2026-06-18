package org.openmrs.module.appointmentscheduling.validator;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.ProviderSchedule;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.sql.Time;
import java.util.Arrays;
import java.util.HashSet;

public class ProviderScheduleValidatorComponentTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private ProviderScheduleValidator providerScheduleValidator;

	private Errors errors;

	@Before
	public void before() throws Exception {
		executeDataSet("standardAppointmentTestDataset.xml");
	}

	private ProviderSchedule newProviderSchedule(DateTime startDate, DateTime endDate) {
		return new ProviderSchedule(null, startDate.toDate(), endDate.toDate(), Time.valueOf("09:00:00"),
		    Time.valueOf("17:00:00"), Context.getProviderService().getProvider(1), Context.getLocationService()
		            .getLocation(1), new HashSet<AppointmentType>(Arrays.asList(Context.getService(
		        AppointmentService.class).getAppointmentType(1))));
	}

	@Test
	public void shouldFailValidationIfStartDateIsNotBeforeEndDate() {

		ProviderSchedule providerSchedule = newProviderSchedule(new DateTime().plusYears(1).plusDays(1), new DateTime()
		        .plusYears(1));

		errors = new BindException(providerSchedule, "test");
		providerScheduleValidator.validate(providerSchedule, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.ProviderSchedule.error.InvalidDateInterval",
		    errors.getFieldError("endDate").getCode());
	}

	@Test
	public void shouldFailValidationIfANewProviderScheduleStartsInThePast() {

		// valid ordering (both dates in the past), so this isolates the past-date check from the ordering check
		ProviderSchedule providerSchedule = newProviderSchedule(new DateTime(2010, 6, 1, 0, 0), new DateTime(2010, 6, 2,
		        0, 0));

		errors = new BindException(providerSchedule, "test");
		providerScheduleValidator.validate(providerSchedule, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.ProviderSchedule.error.dateCannotBeInThePast",
		    errors.getFieldError("startDate").getCode());
	}

	@Test
	public void shouldPassValidationForAValidNewProviderSchedule() {

		ProviderSchedule providerSchedule = newProviderSchedule(new DateTime().plusYears(1), new DateTime().plusYears(1)
		        .plusDays(1));

		errors = new BindException(providerSchedule, "test");
		providerScheduleValidator.validate(providerSchedule, errors);

		Assert.assertEquals(0, errors.getFieldErrorCount());
	}

	@Test
	public void shouldAllowEditingAnExistingProviderScheduleEvenIfItIsNowInThePast() {

		// provider schedule #1 in the dataset started 2019-05-05, long in the past; the past-date rule must
		// only apply to creation, so re-validating this already-saved schedule (as happens on edit) must pass
		ProviderSchedule providerSchedule = Context.getService(AppointmentService.class).getProviderSchedule(1);

		errors = new BindException(providerSchedule, "test");
		providerScheduleValidator.validate(providerSchedule, errors);

		Assert.assertEquals(0, errors.getFieldErrorCount());
	}

}

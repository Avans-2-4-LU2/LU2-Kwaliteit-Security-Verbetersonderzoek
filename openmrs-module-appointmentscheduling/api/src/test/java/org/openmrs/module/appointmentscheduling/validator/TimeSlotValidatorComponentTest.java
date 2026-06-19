package org.openmrs.module.appointmentscheduling.validator;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.util.Arrays;
import java.util.HashSet;

public class TimeSlotValidatorComponentTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private TimeSlotValidator timeSlotValidator;

	private Errors errors;

	@Before
	public void before() throws Exception {
		executeDataSet("standardAppointmentTestDataset.xml");
	}

	private AppointmentBlock saveFutureAppointmentBlock(DateTime start, DateTime end) {
		AppointmentBlock appointmentBlock = new AppointmentBlock();
		appointmentBlock.setStartDate(start.toDate());
		appointmentBlock.setEndDate(end.toDate());
		appointmentBlock.setProvider(Context.getProviderService().getProvider(2)); // has no ProviderSchedule, so availability check is fail-open
		appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
		appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(1))));
		return Context.getService(AppointmentService.class).saveAppointmentBlock(appointmentBlock);
	}

	@Test
	public void shouldFailValidationIfStartDateIsNotBeforeEndDate() {

		AppointmentBlock appointmentBlock = saveFutureAppointmentBlock(new DateTime().plusYears(1),
		    new DateTime().plusYears(1).plusHours(4));

		// both individually within the block's bounds, but reversed relative to each other - isolates the
		// ordering check from the bounds check
		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).plusHours(2).toDate(),
		    new DateTime().plusYears(1).plusHours(1).toDate());

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.TimeSlot.error.InvalidDateInterval",
		    errors.getFieldError("endDate").getCode());
	}

	@Test
	public void shouldFailValidationIfANewTimeSlotStartsInThePast() {

		// appointment block #1 in the dataset spans 2005-01-01 00:00-11:00; 05:00-06:00 is within its bounds and
		// does not overlap the one active sibling time slot (#5, 00:00-01:00), isolating the past-date check
		AppointmentBlock appointmentBlock = Context.getService(AppointmentService.class).getAppointmentBlock(1);
		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime(2005, 1, 1, 5, 0).toDate(), new DateTime(2005, 1,
		        1, 6, 0).toDate());

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.TimeSlot.error.dateCannotBeInThePast",
		    errors.getFieldError("startDate").getCode());
	}

	@Test
	public void shouldFailValidationIfANewTimeSlotFallsOutsideItsAppointmentBlock() {

		AppointmentBlock appointmentBlock = saveFutureAppointmentBlock(new DateTime().plusYears(1),
		    new DateTime().plusYears(1).plusHours(2));

		// starts after the block has already ended
		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).plusHours(3).toDate(),
		    new DateTime().plusYears(1).plusHours(4).toDate());

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.TimeSlot.error.outsideAppointmentBlock",
		    errors.getFieldError("appointmentBlock").getCode());
	}

	@Test
	public void shouldFailValidationIfANewTimeSlotOverlapsASiblingTimeSlot() {

		AppointmentBlock appointmentBlock = saveFutureAppointmentBlock(new DateTime().plusYears(1),
		    new DateTime().plusYears(1).plusHours(4));

		TimeSlot existing = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).toDate(), new DateTime()
		        .plusYears(1).plusHours(1).toDate());
		Context.getService(AppointmentService.class).saveTimeSlot(existing);

		// overlaps the first half of the existing sibling, but still falls within the block's bounds
		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).plusMinutes(30).toDate(),
		    new DateTime().plusYears(1).plusHours(1).plusMinutes(30).toDate());

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(1, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.TimeSlot.error.timeSlotOverlap",
		    errors.getFieldError("startDate").getCode());
	}

	@Test
	public void shouldPassValidationForAValidNewTimeSlot() {

		AppointmentBlock appointmentBlock = saveFutureAppointmentBlock(new DateTime().plusYears(1),
		    new DateTime().plusYears(1).plusHours(4));

		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).toDate(), new DateTime()
		        .plusYears(1).plusHours(1).toDate());

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(0, errors.getFieldErrorCount());
	}

	@Test
	public void shouldAllowEditingAnExistingTimeSlotEvenIfItPredatesTheseRules() {

		// time slot #1 in the dataset (2006-01-01, attached to block #1 which is dated 2005-01-01) falls outside
		// its own appointment block's range and overlaps sibling #2/#4 - none of that was validated when the
		// fixture was created. The new structural checks must only apply to creation, so simply re-validating
		// this already-saved time slot (as happens on edit/void/unvoid) must still pass.
		TimeSlot timeSlot = Context.getService(AppointmentService.class).getTimeSlot(1);

		errors = new BindException(timeSlot, "test");
		timeSlotValidator.validate(timeSlot, errors);

		Assert.assertEquals(0, errors.getFieldErrorCount());
	}

}

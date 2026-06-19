package org.openmrs.module.appointmentscheduling.validator;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.util.Arrays;
import java.util.HashSet;

public class AppointmentBlockValidatorComponentTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private AppointmentBlockValidator appointmentBlockValidator;
	
	private Errors errors;
	
	@Before
	public void before() throws Exception {
		executeDataSet("standardAppointmentTestDataset.xml");
	}
	
	@Test
	public void shouldNotAllowCreationOfOverlappingAppointmentBlock() {

		AppointmentBlock appointmentBlock = new AppointmentBlock();
		// this overlaps with appointment block #1 in the test dataset
		appointmentBlock.setStartDate(new DateTime(2005, 1, 1, 0, 0).toDate());
		appointmentBlock.setEndDate(new DateTime(2005, 1, 2, 0, 0).toDate());

		appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
		appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
		appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
		    1))));

		errors = new BindException(appointmentBlock, "test");
		appointmentBlockValidator.validate(appointmentBlock, errors);

		// this submission is invalid for two independent reasons: it overlaps appointment block #1, and (being
		// a brand new block dated 2005) it also fails the "no past dates" rule. Both are asserted explicitly
		// rather than just counting errors, so this test still demonstrates the overlap check specifically.
		Assert.assertEquals(2, errors.getFieldErrorCount());
		Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.appointmentBlockOverlap",
		    errors.getFieldError("provider").getCode());
		Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.dateCannotBeInThePast",
		    errors.getFieldError("startDate").getCode());
	}

	@Test
	public void shouldAllowCreationOfNonOverlappingAppointmentBlock() {

		AppointmentBlock appointmentBlock = new AppointmentBlock();
		// a future date, so this new block trips neither the overlap check (no provider #1 blocks exist this far out)
		// nor the "no past dates" rule
		appointmentBlock.setStartDate(new DateTime().plusYears(1).toDate());
		appointmentBlock.setEndDate(new DateTime().plusYears(1).plusDays(1).toDate());

		appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
		appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
		appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
		    1))));

		errors = new BindException(appointmentBlock, "test");
		appointmentBlockValidator.validate(appointmentBlock, errors);

		Assert.assertEquals(0, errors.getFieldErrorCount());
	}

    @Test
    public void shouldNotAllowEditingAnAppointmentBlockToRemoveAnAppointmentTypeIfThatTypeAlreadyScheduled() {

        AppointmentBlock appointmentBlock = Context.getService(AppointmentService.class).getAppointmentBlock(1);

        // appointment block #1 already has a appt of type #1
        appointmentBlock.getTypes().remove(Context.getService(AppointmentService.class).getAppointmentType(1));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(2, errors.getFieldErrorCount());
        Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.cannotRemoveTypeFromBlockIfAppointmentScheduled",
                errors.getFieldError("types").getCode());

    }


    @Test
    public void shouldAllowEditingAnAppointmentBlockToRemoveAnAppointmentTypeIfThatTypeIsNotAlreadyScheduled() {

        AppointmentBlock appointmentBlock = Context.getService(AppointmentService.class).getAppointmentBlock(4);

        // appointment block #4 does not have a appt of type #2
        appointmentBlock.getTypes().remove(Context.getService(AppointmentService.class).getAppointmentType(2));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(0, errors.getFieldErrorCount());
    }

    @Test
    public void shouldAllowEditingAnAppointmentBlockToRemoveAnAppointmentTypeIfAppointmentsOfThatTypeHaveBeenCancelled() {

        // first the appointment associated with this block
        Appointment appointment = Context.getService(AppointmentService.class).getAppointment(12);
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED_AND_NEEDS_RESCHEDULE);
        Context.getService(AppointmentService.class).saveAppointment(appointment);
        Context.flushSession();

        AppointmentBlock appointmentBlock = Context.getService(AppointmentService.class).getAppointmentBlock(5);
        appointmentBlock.getTypes().remove(Context.getService(AppointmentService.class).getAppointmentType(1));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        // should be no errors after cancelling
        Assert.assertEquals(0, errors.getFieldErrorCount());
    }

    @Test
    public void shouldFailValidationIfStartDateIsNotBeforeEndDate() {

        AppointmentBlock appointmentBlock = new AppointmentBlock();
        // future, non-overlapping dates, but reversed - so this isolates the ordering check from the
        // overlap/past-date checks
        appointmentBlock.setStartDate(new DateTime().plusYears(1).plusDays(1).toDate());
        appointmentBlock.setEndDate(new DateTime().plusYears(1).toDate());

        appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
        appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
        appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
            1))));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(1, errors.getFieldErrorCount());
        Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.InvalidDateInterval",
            errors.getFieldError("endDate").getCode());
    }

    @Test
    public void shouldFailValidationIfANewAppointmentBlockStartsInThePast() {

        AppointmentBlock appointmentBlock = new AppointmentBlock();
        // in the past, but does not overlap any existing block for provider #1, so this isolates the
        // past-date check from the overlap check
        appointmentBlock.setStartDate(new DateTime(2010, 6, 1, 0, 0).toDate());
        appointmentBlock.setEndDate(new DateTime(2010, 6, 1, 1, 0).toDate());

        appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
        appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
        appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
            1))));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(1, errors.getFieldErrorCount());
        Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.dateCannotBeInThePast",
            errors.getFieldError("startDate").getCode());
    }

    @Test
    public void shouldAllowEditingAnExistingAppointmentBlockEvenThoughItIsNowInThePast() {

        // appointment block #1 in the dataset is dated 2005-01-01, long in the past; the past-date rule must
        // only apply to creation, so editing it (here, just re-saving it unchanged) must still be allowed
        AppointmentBlock appointmentBlock = Context.getService(AppointmentService.class).getAppointmentBlock(1);

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(0, errors.getFieldErrorCount());
    }

    @Test
    public void shouldFailValidationIfProviderHasAScheduleButTheBlockFallsOutsideIt() {

        // provider #1 has a schedule at location #3, 07:00-18:00, valid 2019-2119 (see standardAppointmentTestDataset.xml).
        // a block on a date within that range, but at a time outside the 07:00-18:00 window, must be rejected.
        AppointmentBlock appointmentBlock = new AppointmentBlock();
        appointmentBlock.setStartDate(new DateTime(2030, 6, 1, 20, 0).toDate());
        appointmentBlock.setEndDate(new DateTime(2030, 6, 1, 22, 0).toDate());

        appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
        appointmentBlock.setLocation(Context.getLocationService().getLocation(3));
        appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
            1))));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(1, errors.getFieldErrorCount());
        Assert.assertEquals("appointmentscheduling.AppointmentBlock.error.providerNotAvailable",
            errors.getFieldError("provider").getCode());
    }

    @Test
    public void shouldPassValidationIfTheBlockFallsWithinTheProvidersSchedule() {

        // same provider/location/date-range as above, but the time window (09:00-10:00) falls within the
        // schedule's 07:00-18:00 window
        AppointmentBlock appointmentBlock = new AppointmentBlock();
        appointmentBlock.setStartDate(new DateTime(2030, 6, 1, 9, 0).toDate());
        appointmentBlock.setEndDate(new DateTime(2030, 6, 1, 10, 0).toDate());

        appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
        appointmentBlock.setLocation(Context.getLocationService().getLocation(3));
        appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
            1))));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(0, errors.getFieldErrorCount());
    }

    @Test
    public void shouldPassValidationIfTheProviderHasNoScheduleDefinedAtAll() {

        // provider #2 ("Test Provider") has no ProviderSchedule rows at all in the dataset, so the
        // availability check is fail-open and must not block this booking
        AppointmentBlock appointmentBlock = new AppointmentBlock();
        appointmentBlock.setStartDate(new DateTime(2030, 6, 1, 20, 0).toDate());
        appointmentBlock.setEndDate(new DateTime(2030, 6, 1, 22, 0).toDate());

        appointmentBlock.setProvider(Context.getProviderService().getProvider(2));
        appointmentBlock.setLocation(Context.getLocationService().getLocation(3));
        appointmentBlock.setTypes(new HashSet(Arrays.asList(Context.getService(AppointmentService.class).getAppointmentType(
            1))));

        errors = new BindException(appointmentBlock, "test");
        appointmentBlockValidator.validate(appointmentBlock, errors);

        Assert.assertEquals(0, errors.getFieldErrorCount());
    }

}

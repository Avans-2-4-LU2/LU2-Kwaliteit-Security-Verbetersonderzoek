package org.openmrs.module.appointmentscheduling.api;

import org.hibernate.StaleObjectStateException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Proves the optimistic-locking ("version" column) mechanism added for SR-04 actually detects a
 * concurrent modification, rather than silently letting one writer's change overwrite another's.
 *
 * Two real, separately-connected Hibernate sessions are deliberately not used here. Spring wraps
 * every test method in a transaction that is rolled back afterwards (so other tests never see
 * leftover data); a session opened on a second, independent connection would not see this test's
 * not-yet-committed data at all, so proving the mechanism that way would require suspending that
 * rollback wrapper and manually cleaning up afterwards - extra risk for no extra confidence. The
 * version check Hibernate performs is the same regardless of how the "staleness" arises: it always
 * issues `UPDATE ... WHERE id = ? AND version = ?` and treats zero affected rows as a conflict. So
 * a second writer's commit is simulated here with a raw SQL bump of the version column - from
 * Hibernate's perspective this is indistinguishable from a real concurrent commit.
 */
public class ScheduleConcurrencyTest extends BaseModuleContextSensitiveTest {

	private AppointmentService service;

	@Before
	public void before() throws Exception {
		service = Context.getService(AppointmentService.class);
		executeDataSet("standardAppointmentTestDataset.xml");
	}

	private void bumpVersionDirectlyInTheDatabase(String table, String idColumn, Integer id) throws Exception {
		Context.getAdministrationService().executeSQL(
		    "UPDATE " + table + " SET version = version + 1 WHERE " + idColumn + " = " + id, false);
		Context.flushSession();
		Context.clearSession();
	}

	@Test
	public void shouldRejectSavingAnAppointmentBlockThatWasConcurrentlyModified() throws Exception {

		AppointmentBlock appointmentBlock = new AppointmentBlock();
		appointmentBlock.setStartDate(new DateTime().plusYears(1).toDate());
		appointmentBlock.setEndDate(new DateTime().plusYears(1).plusHours(4).toDate());
		appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
		appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
		appointmentBlock.setTypes(new HashSet<AppointmentType>(Arrays.asList(service.getAppointmentType(1))));
		appointmentBlock = service.saveAppointmentBlock(appointmentBlock);
		Integer id = appointmentBlock.getAppointmentBlockId();
		Assert.assertEquals(Integer.valueOf(0), appointmentBlock.getVersion());

		// reload it the way a second request/user would - a fresh in-memory copy still at version 0
		AppointmentBlock staleCopy = service.getAppointmentBlock(id);

		// simulate another user concurrently saving a change to the same block first
		bumpVersionDirectlyInTheDatabase("appointmentscheduling_appointment_block", "appointment_block_id", id);

		// the stale copy still thinks it is version 0; saving it must fail rather than silently
		// overwrite the concurrent change
		staleCopy.setLocation(Context.getLocationService().getLocation(2));
		try {
			service.saveAppointmentBlock(staleCopy);
			Context.flushSession();
			Assert.fail("Expected a StaleObjectStateException because the row was concurrently modified");
		}
		catch (StaleObjectStateException e) {
			// expected: Hibernate's "UPDATE ... WHERE version = ?" affected zero rows
		}
	}

	@Test
	public void shouldRejectSavingATimeSlotThatWasConcurrentlyModified() throws Exception {

		AppointmentBlock appointmentBlock = new AppointmentBlock();
		appointmentBlock.setStartDate(new DateTime().plusYears(1).toDate());
		appointmentBlock.setEndDate(new DateTime().plusYears(1).plusHours(4).toDate());
		appointmentBlock.setProvider(Context.getProviderService().getProvider(1));
		appointmentBlock.setLocation(Context.getLocationService().getLocation(1));
		appointmentBlock.setTypes(new HashSet<AppointmentType>(Arrays.asList(service.getAppointmentType(1))));
		appointmentBlock = service.saveAppointmentBlock(appointmentBlock);

		TimeSlot timeSlot = new TimeSlot(appointmentBlock, new DateTime().plusYears(1).toDate(), new DateTime()
		        .plusYears(1).plusHours(1).toDate());
		timeSlot = service.saveTimeSlot(timeSlot);
		Integer id = timeSlot.getTimeSlotId();
		Assert.assertEquals(Integer.valueOf(0), timeSlot.getVersion());

		TimeSlot staleCopy = service.getTimeSlot(id);

		bumpVersionDirectlyInTheDatabase("appointmentscheduling_time_slot", "time_slot_id", id);

		staleCopy.setEndDate(new DateTime().plusYears(1).plusHours(2).toDate());
		try {
			service.saveTimeSlot(staleCopy);
			Context.flushSession();
			Assert.fail("Expected a StaleObjectStateException because the row was concurrently modified");
		}
		catch (StaleObjectStateException e) {
			// expected
		}
	}

}

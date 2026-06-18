/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.appointmentscheduling.validator;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.ProviderSchedule;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Validates attributes on the {@link AppointmentBlock} object.
 */
@Handler(supports = { AppointmentBlock.class }, order = 50)
public class AppointmentBlockValidator implements Validator {
	
	/** Log for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Determines if the command object being submitted is a valid type
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean supports(Class c) {
		return c.equals(AppointmentBlock.class);
	}
	
	/**
	 * Checks the form object for any inconsistencies/errors
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 * <strong>Should</strong> pass validation if all required fields have proper values
	 * <strong>Should</strong> fail validation if start date is not before end date
	 * <strong>Should</strong> fail validation if a new appointment block starts in the past
	 * <strong>Should</strong> fail validation if a new appointment block falls outside all of the provider's schedules, when the provider has at least one schedule defined
	 * <strong>Should</strong> pass validation if the provider has no schedules defined at all
	 */

	public void validate(Object obj, Errors errors) {
		AppointmentBlock appointmentBlock = (AppointmentBlock) obj;
		if (appointmentBlock == null) {
			errors.rejectValue("appointmentBlock", "error.general");
		} else {
			ValidationUtils.rejectIfEmpty(errors, "startDate", "appointmentscheduling.AppointmentBlock.emptyStartDate");
			ValidationUtils.rejectIfEmpty(errors, "endDate", "appointmentscheduling.AppointmentBlock.emptyEndDate");
			ValidationUtils.rejectIfEmpty(errors, "location", "appointmentscheduling.AppointmentBlock.emptyLocation");

			if (appointmentBlock.getStartDate() != null && appointmentBlock.getEndDate() != null) {
				if (!appointmentBlock.getStartDate().before(appointmentBlock.getEndDate())) {
					errors.rejectValue("endDate", "appointmentscheduling.AppointmentBlock.error.InvalidDateInterval");
				}
				// only enforced on creation: editing/voiding an already-saved (and possibly now historical) block must remain possible
				// compared at day granularity (not the exact instant) so a block starting "today" is never flagged due to a few milliseconds of test/processing time
				if (appointmentBlock.getAppointmentBlockId() == null
				        && appointmentBlock.getStartDate().before(DateUtils.truncate(new Date(), Calendar.DATE))) {
					errors.rejectValue("startDate", "appointmentscheduling.AppointmentBlock.error.dateCannotBeInThePast");
				}
			}

            if (Context.getService(AppointmentService.class).getOverlappingAppointmentBlocks(appointmentBlock).size() > 0) {
				errors.rejectValue("provider", "appointmentscheduling.AppointmentBlock.error.appointmentBlockOverlap");
			}

			if (appointmentBlock.getAppointmentBlockId() == null && appointmentBlock.getProvider() != null
			        && appointmentBlock.getLocation() != null && appointmentBlock.getStartDate() != null
			        && appointmentBlock.getEndDate() != null) {
				List<ProviderSchedule> schedules = Context.getService(AppointmentService.class)
				        .getProviderSchedulesByConstraints(appointmentBlock.getLocation(), appointmentBlock.getProvider(), null);
				// fail-open: only enforced once the provider actually has at least one schedule defined. If none
				// exist, there is no basis to judge availability, so the booking is allowed (see
				// documentation/mitigations/schedule-integrity-controls.md for the rationale).
				if (!schedules.isEmpty()) {
					boolean providerAvailable = false;
					for (ProviderSchedule schedule : schedules) {
						if (coversDateRange(schedule, appointmentBlock) && coversTimeOfDay(schedule, appointmentBlock)) {
							providerAvailable = true;
							break;
						}
					}
					if (!providerAvailable) {
						errors.rejectValue("provider", "appointmentscheduling.AppointmentBlock.error.providerNotAvailable");
					}
				}
			}

            Set<AppointmentType> types = appointmentBlock.getTypes();
            if (types == null) {
                ValidationUtils.rejectIfEmpty(errors, "types", "appointmentscheduling.AppointmentBlock.emptyTypes");
            }

            if (appointmentBlock.getId() != null) {   // only test this on appointment blocks that have previously been saved (otherwise will get transient exception)
                for (TimeSlot timeSlot : Context.getService(AppointmentService.class).getTimeSlotsInAppointmentBlock(appointmentBlock)) {
                    for (Appointment appointment : Context.getService(AppointmentService.class).getAppointmentsInTimeSlotThatAreNotCancelled(timeSlot)) {
                        if (!types.contains(appointment.getAppointmentType())) {
                            errors.rejectValue("types", "appointmentscheduling.AppointmentBlock.error.cannotRemoveTypeFromBlockIfAppointmentScheduled");
                        }
                    }
                }
            }
		}
	}

	private boolean coversDateRange(ProviderSchedule schedule, AppointmentBlock appointmentBlock) {
		return !schedule.getStartDate().after(appointmentBlock.getStartDate())
		        && !schedule.getEndDate().before(appointmentBlock.getEndDate());
	}

	private boolean coversTimeOfDay(ProviderSchedule schedule, AppointmentBlock appointmentBlock) {
		return secondsOfDay(appointmentBlock.getStartDate()) >= secondsOfDay(schedule.getStartTime())
		        && secondsOfDay(appointmentBlock.getEndDate()) <= secondsOfDay(schedule.getEndTime());
	}

	private int secondsOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
	}
}

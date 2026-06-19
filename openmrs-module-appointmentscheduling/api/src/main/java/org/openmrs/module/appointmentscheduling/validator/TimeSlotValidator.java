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
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;

import java.util.Calendar;
import java.util.Date;

/**
 * Validates attributes on the {@link TimeSlot} object.
 */
@Handler(supports = { TimeSlot.class }, order = 50)
public class TimeSlotValidator implements Validator {
	
	/** Log for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Determines if the command object being submitted is a valid type
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean supports(Class c) {
		return c.equals(TimeSlot.class);
	}
	
	/**
	 * Checks the form object for any inconsistencies/errors
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 * <strong>Should</strong> fail validation if name is null or empty or whitespace
	 * <strong>Should</strong> pass validation if all required fields have proper values
	 * <strong>Should</strong> fail validation if a new time slot's start date is not before its end date
	 * <strong>Should</strong> fail validation if a new time slot starts in the past
	 * <strong>Should</strong> fail validation if a new time slot falls outside its appointment block
	 * <strong>Should</strong> fail validation if a new time slot overlaps another time slot in the same appointment block
	 */

	public void validate(Object obj, Errors errors) {
		TimeSlot timeSlot = (TimeSlot) obj;
		if (timeSlot == null) {
			errors.rejectValue("timeSlot", "error.general");
		} else {
			ValidationUtils.rejectIfEmpty(errors, "startDate", "appointmentscheduling.TimeSlot.emptyStartDate");
			ValidationUtils.rejectIfEmpty(errors, "endDate", "appointmentscheduling.TimeSlot.emptyEndDate");
			ValidationUtils.rejectIfEmpty(errors, "appointmentBlock", "appointmentscheduling.TimeSlot.emptyBlock");

			// the structural checks below are only enforced on creation. Pre-existing/legacy time slots are not
			// retroactively re-validated against them, so that editing, voiding, or unvoiding an already-saved
			// time slot remains possible even if it predates these rules (e.g. older fixture/migrated data).
			if (timeSlot.getTimeSlotId() == null && timeSlot.getStartDate() != null && timeSlot.getEndDate() != null) {
				if (!timeSlot.getStartDate().before(timeSlot.getEndDate())) {
					errors.rejectValue("endDate", "appointmentscheduling.TimeSlot.error.InvalidDateInterval");
				}
				// compared at day granularity (not the exact instant) so a time slot starting "today" is never flagged due to a few milliseconds of test/processing time
				if (timeSlot.getStartDate().before(DateUtils.truncate(new Date(), Calendar.DATE))) {
					errors.rejectValue("startDate", "appointmentscheduling.TimeSlot.error.dateCannotBeInThePast");
				}

				AppointmentBlock appointmentBlock = timeSlot.getAppointmentBlock();
				if (appointmentBlock != null) {
					if ((appointmentBlock.getStartDate() != null && timeSlot.getStartDate().before(appointmentBlock.getStartDate()))
					        || (appointmentBlock.getEndDate() != null && timeSlot.getEndDate().after(appointmentBlock.getEndDate()))) {
						errors.rejectValue("appointmentBlock", "appointmentscheduling.TimeSlot.error.outsideAppointmentBlock");
					}

					for (TimeSlot sibling : Context.getService(AppointmentService.class).getTimeSlotsInAppointmentBlock(
					    appointmentBlock)) {
						if (timeSlot.getStartDate().before(sibling.getEndDate())
						        && sibling.getStartDate().before(timeSlot.getEndDate())) {
							errors.rejectValue("startDate", "appointmentscheduling.TimeSlot.error.timeSlotOverlap");
							break;
						}
					}
				}
			}
		}
	}
}

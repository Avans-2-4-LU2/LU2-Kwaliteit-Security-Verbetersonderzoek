package org.openmrs.module.appointmentscheduling.validator;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.module.appointmentscheduling.AppointmentBlock;
import org.openmrs.module.appointmentscheduling.ProviderSchedule;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Validates attributes on the {@link AppointmentBlock} object.
 */
@Handler(supports = {ProviderSchedule.class}, order = 50)
public class ProviderScheduleValidator implements Validator {

    /**
     * Log for this class and subclasses
     */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Determines if the command object being submitted is a valid type
     *
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public boolean supports(Class c) {
        return c.equals(ProviderSchedule.class);
    }

    /**
     * Checks the form object for any inconsistencies/errors
     *
     * <strong>Should</strong> pass validation if all required fields have proper values
     * <strong>Should</strong> fail validation if start date is not before end date
     * @see org.springframework.validation.Validator#validate(java.lang.Object,
     * org.springframework.validation.Errors)
     */

    public void validate(Object obj, Errors errors) {
        ProviderSchedule providerSchedule = (ProviderSchedule) obj;
        if (providerSchedule == null) {
            errors.rejectValue("providerSchedule", "error.general");
        } else {
            ValidationUtils.rejectIfEmpty(errors, "startDate", "appointmentscheduling.ProviderSchedule.emptyStartDate");
            ValidationUtils.rejectIfEmpty(errors, "endDate", "appointmentscheduling.ProviderSchedule.emptyEndDate");
            ValidationUtils.rejectIfEmpty(errors, "startTime", "appointmentscheduling.ProviderSchedule.emptyStartTime");
            ValidationUtils.rejectIfEmpty(errors, "endTime", "appointmentscheduling.ProviderSchedule.emptyEndTime");
            ValidationUtils.rejectIfEmpty(errors, "location", "appointmentscheduling.ProviderSchedule.emptyLocation");

            if (providerSchedule.getStartDate() != null && providerSchedule.getEndDate() != null) {
                if (!providerSchedule.getStartDate().before(providerSchedule.getEndDate())) {
                    errors.rejectValue("endDate", "appointmentscheduling.ProviderSchedule.error.InvalidDateInterval");
                }
                // only enforced on creation: editing an already-saved (and possibly now historical) schedule must remain possible
                // compared at day granularity (not the exact instant) so a schedule starting "today" is never flagged due to a few milliseconds of test/processing time
                if (providerSchedule.getProviderScheduleId() == null
                        && providerSchedule.getStartDate().before(DateUtils.truncate(new Date(), Calendar.DATE))) {
                    errors.rejectValue("startDate", "appointmentscheduling.ProviderSchedule.error.dateCannotBeInThePast");
                }
            }

            Set<AppointmentType> types = providerSchedule.getTypes();
            if (types == null) {
                ValidationUtils.rejectIfEmpty(errors, "types", "appointmentscheduling.ProviderSchedule.emptyTypes");
            }
        }
    }
}

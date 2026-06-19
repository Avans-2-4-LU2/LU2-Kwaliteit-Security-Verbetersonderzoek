package org.openmrs.module.appointmentscheduling.api.impl;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.AppointmentSchedulingConstants;
import org.openmrs.module.appointmentscheduling.api.AppointmentConfidentialityService;

import java.util.ArrayList;
import java.util.List;

public class AppointmentConfidentialityServiceImpl
        implements AppointmentConfidentialityService {

    @Override
    public Appointment filterConfidentialAppointmentIfNotAuthorized(
            Appointment appointment) {

        if (isConfidentialAppointment(appointment)
                && !Context.hasPrivilege(
                AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS)) {

            throw new APIAuthenticationException(
                    "Privilege required: "
                            + AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS);
        }

        return appointment;
    }

    @Override
    public List<Appointment> removeConfidentialAppointmentsIfNotAuthorized(
            List<Appointment> appointments) {

        if (appointments == null
                || Context.hasPrivilege(
                AppointmentSchedulingConstants.PRIVILEGE_VIEW_CONFIDENTIAL_APPOINTMENT_DETAILS)) {
            return appointments;
        }

        List<Appointment> filtered = new ArrayList<Appointment>();

        for (Appointment appointment : appointments) {
            if (!isConfidentialAppointment(appointment)) {
                filtered.add(appointment);
            }
        }

        return filtered;
    }

    private boolean isConfidentialAppointment(Appointment appointment) {
        return appointment != null
                && appointment.getAppointmentType() != null
                && appointment.getAppointmentType().isConfidential();
    }
}

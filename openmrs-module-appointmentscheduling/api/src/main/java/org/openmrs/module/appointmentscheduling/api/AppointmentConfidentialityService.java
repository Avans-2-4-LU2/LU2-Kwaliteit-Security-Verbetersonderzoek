package org.openmrs.module.appointmentscheduling.api;

import org.openmrs.module.appointmentscheduling.Appointment;

import java.util.List;

public interface AppointmentConfidentialityService {

    Appointment filterConfidentialAppointmentIfNotAuthorized(Appointment appointment);

    List<Appointment> removeConfidentialAppointmentsIfNotAuthorized(
            List<Appointment> appointments);
}

package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;

/**
 * Puerto de entrada (Caso de Uso): Agendamiento y logística de recogida de la donación.
 */
public interface ScheduleDonationPickupUseCase {

    DonationRequest schedulePickup(SchedulePickupCommand command);
}

package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;

/**
 * Puerto de entrada (Caso de Uso): Edición, venta, retiro y destinación a donación de publicaciones.
 */
public interface ManagePublicationUseCase {

    SecondHandPublication edit(EditPublicationCommand command);

    SecondHandPublication markAsSold(Long userId, Long publicationId);

    void retire(Long userId, Long publicationId);

    SecondHandPublication markForDonation(Long userId, Long publicationId);
}

package edu.dosw.styleradar.donaciones.domain.port.in;

import edu.dosw.styleradar.donaciones.domain.model.PublicacionSegundaMano;

/**
 * Caso de uso (Puerto de Entrada): Edición, venta, retiro y asignación a donación de publicaciones.
 */
public interface GestionarPublicacionUseCase {

    PublicacionSegundaMano editar(EditarPublicacionCommand command);

    PublicacionSegundaMano marcarComoVendida(Long usuarioId, Long publicacionId);

    void retirar(Long usuarioId, Long publicacionId);

    PublicacionSegundaMano destinarADonacion(Long usuarioId, Long publicacionId);
}

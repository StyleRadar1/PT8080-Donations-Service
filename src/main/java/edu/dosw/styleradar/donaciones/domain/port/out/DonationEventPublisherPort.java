package edu.dosw.styleradar.donaciones.domain.port.out;

/**
 * Puerto de salida (SPI) para la publicación de eventos de dominio hacia el broker de mensajería (RabbitMQ).
 */
public interface DonationEventPublisherPort {

    void publishEvent(Object event);

    /**
     * Publica un evento esperando confirmación efectiva (ACK) del broker y validando
     * que no haya sido devuelto como no enrutable.
     */
    default void publishEventConfirmed(Object event) {
        publishEvent(event);
    }
}

package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.messaging.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formateador centralizado de fechas para eventos de integración RabbitMQ.
 * Serializa marcas de tiempo en formato ISO-8601 sin zona horaria ({@code yyyy-MM-dd'T'HH:mm:ss})
 * compatible con la deserialización {@link LocalDateTime} en {@code ms-notificaciones}.
 */
public final class IntegrationDateFormatter {

    public static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    public static final DateTimeFormatter LOCAL_ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private IntegrationDateFormatter() {
    }

    public static String formatLocalDateTime(Instant instant) {
        Instant effective = instant != null ? instant : Instant.now();
        return LocalDateTime.ofInstant(effective, BOGOTA_ZONE).format(LOCAL_ISO_FORMATTER);
    }

    public static String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return nowLocalDateTime();
        }
        return dateTime.format(LOCAL_ISO_FORMATTER);
    }

    public static String nowLocalDateTime() {
        return formatLocalDateTime(Instant.now());
    }
}

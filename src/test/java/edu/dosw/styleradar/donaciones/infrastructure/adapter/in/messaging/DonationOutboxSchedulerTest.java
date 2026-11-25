package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.model.DonationOutboxEvent;
import edu.dosw.styleradar.donaciones.domain.model.OutboxEventStatus;
import edu.dosw.styleradar.donaciones.domain.port.in.HandleFoundationDeletedUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.mapper.DonationOutboxPersistenceMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - DonationOutboxScheduler, DonationOutboxEvent y Mapper")
class DonationOutboxSchedulerTest {

    @Mock
    private HandleFoundationDeletedUseCase handleFoundationDeletedUseCase;

    @InjectMocks
    private DonationOutboxScheduler scheduler;

    @Test
    @DisplayName("Debe ejecutar reintento periódico sin propagar excepciones si RabbitMQ sigue caído")
    void shouldRetryPendingOutboxEventsSafely() {
        when(handleFoundationDeletedUseCase.dispatchPendingOutboxEvents())
                .thenReturn(2)
                .thenReturn(0)
                .thenThrow(new RuntimeException("Broker unavailable"));

        assertDoesNotThrow(() -> {
            scheduler.retryPendingOutboxEvents();
            scheduler.retryPendingOutboxEvents();
            scheduler.retryPendingOutboxEvents();
        });

        verify(handleFoundationDeletedUseCase, times(3)).dispatchPendingOutboxEvents();
    }

    @Test
    @DisplayName("Debe truncar mensajes de error extensos y mapear nulos en Outbox")
    void shouldHandleOutboxDomainAndMapperEdgeCases() {
        DonationOutboxEvent evt = DonationOutboxEvent.builder()
                .requestId(1L)
                .foundationId(3L)
                .donorId(101L)
                .rejectionReason("Eliminada")
                .eventType(DonationOutboxEvent.TYPE_DONATION_CANCELLED_FOUNDATION_DELETED)
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .build();

        evt.recordFailure(null);
        assertThat(evt.getLastError()).isEqualTo("Error desconocido al publicar en broker");
        assertThat(evt.getAttempts()).isEqualTo(1);

        evt.recordFailure("   ");
        assertThat(evt.getLastError()).isEqualTo("Error desconocido al publicar en broker");

        String longError = "X".repeat(600);
        evt.recordFailure(longError);
        assertThat(evt.getLastError()).hasSize(500);

        DonationOutboxPersistenceMapper mapper = new DonationOutboxPersistenceMapper();
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}

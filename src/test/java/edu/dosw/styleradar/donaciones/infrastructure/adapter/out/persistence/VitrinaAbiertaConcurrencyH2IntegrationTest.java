package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.styleradar.donaciones.domain.exception.ConcurrentDonationClaimException;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity.SecondHandPublicationEntity;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de concurrencia optimista sobre base de datos de pruebas (perfil test).
 * Valida que ante reclamos concurrentes de dos fundaciones sobre la misma prenda en Vitrina Abierta,
 * exactamente una gane la prenda y la otra reciba conflicto de concurrencia.
 */
@SpringBootTest
@ActiveProfiles("test")
class VitrinaAbiertaConcurrencyH2IntegrationTest {

    @MockBean
    private DonationEventPublisherPort donationEventPublisherPort;

    @Autowired
    private ClaimDonationUseCase claimDonationUseCase;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    @Test
    @DisplayName("Concurrencia Vitrina Abierta: Dos fundaciones reclaman al mismo tiempo -> Exactamente una gana y la otra recibe conflicto")
    void shouldHandleConcurrentClaimsOptimistically() throws Exception {
        // 1. Crear una prenda de donación en estado AVAILABLE
        SecondHandPublicationEntity entity = SecondHandPublicationEntity.builder()
                .userId(101L)
                .garmentName("Chaqueta Térmica Invierno")
                .description("Chaqueta para donación benéfica a comunidad vulnerable")
                .garmentType(GarmentType.UPPER)
                .brand("Columbia")
                .color("Negro")
                .style(Style.URBAN)
                .size(Size.L)
                .conservationStatus(ConservationStatus.GOOD_CONDITION)
                .price(BigDecimal.ZERO)
                .esDonacion(true)
                .localidad(LocalidadBogota.CHAPINERO)
                .status(PublicationStatus.AVAILABLE)
                .publishedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SecondHandPublicationEntity saved = publicationRepository.saveAndFlush(entity);
        Long pubId = saved.getId();

        // 2. Disparar dos hilos concurrentes reclamando la misma prenda
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Throwable> errors = new java.util.concurrent.CopyOnWriteArrayList<>();

        Runnable claimTaskFoundation1 = () -> {
            try {
                startSignal.await();
                claimDonationUseCase.claimDonation(new ClaimDonationCommand(pubId, 501L, "Reclamo Fundación 1"));
                successCount.incrementAndGet();
            } catch (ConcurrentDonationClaimException | DomainConflictException | OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable e) {
                errors.add(e);
            }
        };

        Runnable claimTaskFoundation2 = () -> {
            try {
                startSignal.await();
                claimDonationUseCase.claimDonation(new ClaimDonationCommand(pubId, 502L, "Reclamo Fundación 2"));
                successCount.incrementAndGet();
            } catch (ConcurrentDonationClaimException | DomainConflictException | OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable e) {
                errors.add(e);
            }
        };

        Future<?> f1 = executor.submit(claimTaskFoundation1);
        Future<?> f2 = executor.submit(claimTaskFoundation2);

        // Liberar ambos hilos al mismo tiempo
        startSignal.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        if (!errors.isEmpty()) {
            errors.forEach(Throwable::printStackTrace);
        }

        // 3. Verificación de invariantes
        assertThat(successCount.get())
                .as("Exactamente una fundación debe ganar el reclamo de la donación")
                .isEqualTo(1);

        assertThat(conflictCount.get())
                .as("Exactamente una fundación debe ser rechazada por conflicto de concurrencia")
                .isEqualTo(1);

        // 4. Estado final de la publicación en la BD
        SecondHandPublicationEntity reloaded = publicationRepository.findById(pubId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PublicationStatus.IN_DONATION);
    }
}

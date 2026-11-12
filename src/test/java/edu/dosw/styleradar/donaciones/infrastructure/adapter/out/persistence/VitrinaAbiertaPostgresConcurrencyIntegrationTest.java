package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
 * Prueba de integración de concurrencia real sobre PostgreSQL 16 con Testcontainers.
 * Valida el control de concurrencia optimista (@Version) en los reclamos de la Vitrina Abierta.
 * Se ejecuta automáticamente cuando el entorno de ejecución cuenta con el motor de Docker disponible.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("isDockerAvailable")
class VitrinaAbiertaPostgresConcurrencyIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("styleradar_donations_test")
            .withUsername("styleradar_user")
            .withPassword("styleradar_secret");

    @MockBean
    private DonationEventPublisherPort donationEventPublisherPort;

    @Autowired
    private ClaimDonationUseCase claimDonationUseCase;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    @BeforeAll
    static void checkDocker() {
        System.setProperty("api.version", "1.44");
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"));
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        if (!isCi && !dockerAvailable) {
            Assumptions.abort("Docker no está disponible para ejecutar pruebas con PostgreSQL Testcontainers.");
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Test
    @DisplayName("Concurrencia Real PostgreSQL: Dos fundaciones reclaman al mismo tiempo en Vitrina Abierta -> Exactamente una gana (HTTP 201) y la otra recibe conflicto")
    void shouldHandleConcurrentClaimsOnRealPostgreSQL() throws Exception {
        // 1. Crear una prenda de donación en estado AVAILABLE sobre la base de datos PostgreSQL real
        SecondHandPublicationEntity entity = SecondHandPublicationEntity.builder()
                .userId(101L) // Donante
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

        Runnable claimTaskFoundation1 = () -> {
            try {
                startSignal.await();
                claimDonationUseCase.claimDonation(new ClaimDonationCommand(pubId, 501L, "Reclamo Fundación 1"));
                successCount.incrementAndGet();
            } catch (ConcurrentDonationClaimException | DomainConflictException | OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                // Otro error
            }
        };

        Runnable claimTaskFoundation2 = () -> {
            try {
                startSignal.await();
                claimDonationUseCase.claimDonation(new ClaimDonationCommand(pubId, 502L, "Reclamo Fundación 2"));
                successCount.incrementAndGet();
            } catch (ConcurrentDonationClaimException | DomainConflictException | OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                // Otro error
            }
        };

        Future<?> f1 = executor.submit(claimTaskFoundation1);
        Future<?> f2 = executor.submit(claimTaskFoundation2);

        // Liberar ambos hilos al mismo instante exacto
        startSignal.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Verificación de invariantes de concurrencia
        assertThat(successCount.get())
                .as("Exactamente una fundación debe ganar el reclamo de la donación")
                .isEqualTo(1);

        assertThat(conflictCount.get())
                .as("La fundación perdedora debe recibir un conflicto de concurrencia optimista")
                .isEqualTo(1);

        SecondHandPublicationEntity updated = publicationRepository.findById(pubId).orElseThrow();
        assertThat(updated.getStatus())
                .as("La publicación debe quedar comprometida en estado IN_DONATION")
                .isEqualTo(PublicationStatus.IN_DONATION);
        assertThat(updated.getVersion())
                .as("El número de versión en PostgreSQL debe haber sido incrementado por el bloqueo optimista")
                .isGreaterThanOrEqualTo(1L);
    }
}

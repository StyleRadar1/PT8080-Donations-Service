package edu.dosw.styleradar.donaciones.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import edu.dosw.styleradar.donaciones.domain.event.DonationRequestCreatedEvent;
import edu.dosw.styleradar.donaciones.domain.exception.DomainConflictException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.DeliveryMode;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequestStatus;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.model.Garment;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationSearchCriteria;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.SecondHandPublication;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.ClaimDonationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ConfirmDonationReceiptUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreateDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationCommand;
import edu.dosw.styleradar.donaciones.domain.port.in.CreatePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.DecideDonationRequestUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPublicationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ManagePublicationUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.ScheduleDonationPickupUseCase;
import edu.dosw.styleradar.donaciones.domain.port.in.SchedulePickupCommand;
import edu.dosw.styleradar.donaciones.domain.port.out.DonationEventPublisherPort;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataDonationRequestRepository;
import edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.repository.SpringDataPublicationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba de integración integral de los 7 escenarios del ciclo completo de donaciones
 * sobre base de datos relacional H2 (perfil test).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integración E2E - Ciclo Completo de Donaciones y Segunda Vida")
class DonationCompleteFlowIntegrationTest {

    @MockBean
    private DonationEventPublisherPort eventPublisherPort;

    @MockBean
    private FoundationCatalogPort foundationCatalogPort;

    @Autowired
    private CreatePublicationUseCase createPublicationUseCase;

    @Autowired
    private GetPublicationsUseCase getPublicationsUseCase;

    @Autowired
    private ManagePublicationUseCase managePublicationUseCase;

    @Autowired
    private CreateDonationRequestUseCase createDonationRequestUseCase;

    @Autowired
    private ClaimDonationUseCase claimDonationUseCase;

    @Autowired
    private DecideDonationRequestUseCase decideDonationRequestUseCase;

    @Autowired
    private ScheduleDonationPickupUseCase scheduleDonationPickupUseCase;

    @Autowired
    private ConfirmDonationReceiptUseCase confirmDonationReceiptUseCase;

    @Autowired
    private SpringDataDonationRequestRepository donationRequestRepository;

    @Autowired
    private SpringDataPublicationRepository publicationRepository;

    @BeforeEach
    void setUp() {
        donationRequestRepository.deleteAll();
        publicationRepository.deleteAll();

        FoundationSummary foundation501 = new FoundationSummary(
                501L, "Fundación Abrigo Solidario", "Chapinero", "Abrigo comunitario",
                "Calle 85 # 15-32", "6017428899", "contacto@abrigo.org",
                List.of("SUPERIOR", "CALZADO", "Chaquetas"), "Niñez vulnerable",
                "08:00-17:00", "Prendas limpias", true, 901L
        );
        FoundationSummary foundation502 = new FoundationSummary(
                502L, "Fundación Calzado Feliz", "Usaquén", "Calzado comunitario",
                "Carrera 7 # 120-10", "6017429900", "contacto@calzado.org",
                List.of("CALZADO", "Calzado"), "Adulto mayor",
                "09:00-17:00", "Calzado en buen estado", true, 902L
        );

        when(foundationCatalogPort.findFoundationById(501L)).thenReturn(Optional.of(foundation501));
        when(foundationCatalogPort.findFoundationById(502L)).thenReturn(Optional.of(foundation502));
        when(foundationCatalogPort.isFoundationRepresentative(901L, 501L)).thenReturn(true);
        when(foundationCatalogPort.isFoundationRepresentative(902L, 502L)).thenReturn(true);
    }

    @Test
    @DisplayName("Debe completar los 7 escenarios del ciclo benéfico de donaciones y vitrina abierta")
    void shouldExecuteCompleteSevenScenariosDonationLifecycle() {
        // 1. Publicación directa de una prenda como donación (esDonacion=true, price=0, AVAILABLE)
        Garment jacket = Garment.builder()
                .name("Chaqueta Térmica Bogotá")
                .description("Chaqueta abrigada para clima frío en Bogotá")
                .type(GarmentType.CHAQUETAS)
                .brand("North Face")
                .color("Azul")
                .style(Style.URBAN)
                .build();

        SecondHandPublication directDonation = createPublicationUseCase.create(new CreatePublicationCommand(
                101L, jacket, Size.TALLA_38, ConservationStatus.LIKE_NEW, BigDecimal.ZERO,
                true, LocalidadBogota.CHAPINERO, List.of("https://cdn.styleradar.com/chaqueta.jpg")
        ));

        assertThat(directDonation)
                .returns(true, SecondHandPublication::isEsDonacion)
                .returns(PublicationStatus.AVAILABLE, SecondHandPublication::getStatus)
                .returns(Size.TALLA_38, SecondHandPublication::getSize);
        assertThat(directDonation.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(directDonation.getGarment().getType()).isEqualTo(GarmentType.CHAQUETAS);

        // Verificar catálogo general SR-RF-36 con filtro de talla numérica y categoría
        List<SecondHandPublication> catalog = getPublicationsUseCase.search(PublicationSearchCriteria.builder()
                .type(GarmentType.CHAQUETAS)
                .size(Size.TALLA_38)
                .esDonacion(true)
                .build());
        assertThat(catalog).extracting(SecondHandPublication::getId).containsExactly(directDonation.getId());

        // 2. Conversión posterior de publicación de venta a donación (PATCH /{id}/donation)
        Garment shoes = Garment.builder()
                .name("Tenis Urbanos")
                .description("Calzado deportivo en buen estado")
                .type(GarmentType.CALZADO)
                .brand("Adidas")
                .color("Blanco")
                .style(Style.SPORTY)
                .build();

        SecondHandPublication salePub = createPublicationUseCase.create(new CreatePublicationCommand(
                102L, shoes, Size.TALLA_40, ConservationStatus.GOOD_CONDITION, new BigDecimal("85000.00"),
                false, LocalidadBogota.SUBA, List.of("https://cdn.styleradar.com/tenis.jpg")
        ));
        assertThat(salePub)
                .returns(false, SecondHandPublication::isEsDonacion)
                .returns(PublicationStatus.AVAILABLE, SecondHandPublication::getStatus);

        SecondHandPublication converted = managePublicationUseCase.markForDonation(102L, salePub.getId());
        assertThat(converted)
                .returns(true, SecondHandPublication::isEsDonacion)
                .returns(PublicationStatus.IN_DONATION, SecondHandPublication::getStatus);
        assertThat(converted.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);

        // 3. Selección de fundación y creación de solicitud con propuesta de entrega (POST /api/v1/donations)
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(2);
        DonationRequest createdRequest = createDonationRequestUseCase.create(new CreateDonationRequestCommand(
                102L,
                converted.getId(),
                501L,
                "Propongo entregar los tenis el fin de semana",
                DeliveryMode.DROP_OFF_POINT,
                "Calle 85 # 15-32, Chapinero, Bogotá",
                start,
                end
        ));
        assertThat(createdRequest)
                .returns(DonationRequestStatus.PENDING, DonationRequest::getStatus)
                .returns(DeliveryMode.DROP_OFF_POINT, DonationRequest::getDeliveryMode);
        verify(eventPublisherPort, atLeastOnce()).publishEvent(argThat((Object ev) ->
                ev instanceof DonationRequestCreatedEvent createdEv
                        && Long.valueOf(901L).equals(createdEv.representanteId())));

        // 6a. Rechazo sin motivo debe fallar; rechazo con motivo revierte la prenda a AVAILABLE en vitrina abierta
        Long createdRequestId = createdRequest.getId();
        assertThatThrownBy(() -> decideDonationRequestUseCase.decide(501L, createdRequestId, false, "   "))
                .isInstanceOf(InvalidDonationRequestException.class);

        DonationRequest rejected = decideDonationRequestUseCase.decide(
                501L, createdRequestId, false, "No hay cupo para calzado esta semana");
        assertThat(rejected.getStatus()).isEqualTo(DonationRequestStatus.REJECTED);

        SecondHandPublication reverted = getPublicationsUseCase.getById(converted.getId());
        assertThat(reverted)
                .returns(PublicationStatus.AVAILABLE, SecondHandPublication::getStatus)
                .returns(true, SecondHandPublication::isEsDonacion);

        // Ahora vuelve a postular hacia fundación 502 y ésta aprueba la solicitud (6b)
        managePublicationUseCase.markForDonation(102L, reverted.getId());
        DonationRequest secondRequest = createDonationRequestUseCase.create(new CreateDonationRequestCommand(
                102L,
                reverted.getId(),
                502L,
                "Segunda postulación hacia fundación especializada en calzado",
                DeliveryMode.HOME_PICKUP,
                "Calle 145 # 92-10, Suba, Bogotá",
                start,
                end
        ));
        DonationRequest approvedRequest = decideDonationRequestUseCase.decide(502L, secondRequest.getId(), true, null);
        assertThat(approvedRequest.getStatus()).isEqualTo(DonationRequestStatus.APPROVED);

        // 4. Reclamo directo de la prenda #1 (directDonation) desde la Vitrina Abierta (POST /api/v1/donations/claims)
        DonationRequest claimed = claimDonationUseCase.claimDonation(new ClaimDonationCommand(
                directDonation.getId(), 501L, "Reclamamos la chaqueta para nuestra campaña de invierno"
        ));
        assertThat(claimed.getStatus()).isEqualTo(DonationRequestStatus.APPROVED);
        assertThat(getPublicationsUseCase.getById(directDonation.getId()).getStatus())
                .isEqualTo(PublicationStatus.IN_DONATION);

        // 5. Validación de conflicto cuando una segunda fundación intenta reclamar la misma prenda (409 Conflict)
        ClaimDonationCommand duplicateClaimCommand = new ClaimDonationCommand(
                directDonation.getId(), 502L, "Segundo reclamo concurrente"
        );
        assertThatThrownBy(() -> claimDonationUseCase.claimDonation(duplicateClaimCommand))
                .isInstanceOf(DomainConflictException.class);

        // 7. Programación de recogida (PATCH /{id}/pickup) y confirmación de recepción (PATCH /{id}/confirmation)
        DonationRequest inPickup = scheduleDonationPickupUseCase.schedulePickup(new SchedulePickupCommand(
                101L,
                claimed.getId(),
                "Punto de Acopio Chapinero: Calle 85 # 15-32, Bogotá",
                start,
                end
        ));
        assertThat(inPickup.getStatus()).isEqualTo(DonationRequestStatus.IN_PICKUP);

        DonationRequest delivered = confirmDonationReceiptUseCase.confirmReceipt(501L, claimed.getId());
        assertThat(delivered.getStatus()).isEqualTo(DonationRequestStatus.DELIVERED);
        assertThat(delivered.getReceivedAt()).isNotNull();

        SecondHandPublication finalPublication = getPublicationsUseCase.getById(directDonation.getId());
        assertThat(finalPublication.getStatus()).isEqualTo(PublicationStatus.DONATED);
    }
}

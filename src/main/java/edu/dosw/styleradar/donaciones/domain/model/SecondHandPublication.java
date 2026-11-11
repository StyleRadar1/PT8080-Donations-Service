package edu.dosw.styleradar.donaciones.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.exception.InvalidStateTransitionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agregado raíz que representa una publicación de prenda usada en el módulo de Economía Circular.
 * Encapsula la lógica del ciclo de vida (publicar, editar, retirar, vender, donar) y sus invariantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondHandPublication {

    /** Número máximo permitido de fotografías por publicación. */
    public static final int MAX_PHOTOS = 4;

    private Long id;
    private Long userId;
    private Garment garment;
    private Size size;
    private ConservationStatus conservationStatus;
    private BigDecimal price;
    private PublicationStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<String> photos = new ArrayList<>();

    /**
     * Valida las invariantes fundamentales de la publicación.
     */
    public void validateInvariants() {
        if (userId == null) {
            throw new InvalidPublicationException("El ID del usuario propietario es obligatorio");
        }
        if (garment == null) {
            throw new InvalidPublicationException("La prenda asociada es obligatoria");
        }
        garment.validateInvariants();

        if (size == null) {
            throw new InvalidPublicationException("La talla de la prenda es obligatoria");
        }
        if (conservationStatus == null) {
            throw new InvalidPublicationException("El estado de conservación es obligatorio");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPublicationException("El precio de la prenda usada debe ser mayor o igual a 0");
        }
        if (photos != null && photos.size() > MAX_PHOTOS) {
            throw new InvalidPublicationException("Una publicación no puede contener más de " + MAX_PHOTOS + " fotografías");
        }
    }

    /**
     * Publica la prenda haciéndola disponible en el catálogo de reventa.
     */
    public void publish() {
        validateInvariants();
        this.status = PublicationStatus.AVAILABLE;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.publishedAt = now;
        this.updatedAt = now;
    }

    /**
     * Modifica los datos de la publicación verificando que no se encuentre en un estado terminal o bloqueante.
     */
    public void edit(Garment newGarment, Size newSize, ConservationStatus newConservationStatus,
                     BigDecimal newPrice, List<String> newPhotos) {
        if (status == PublicationStatus.SOLD) {
            throw new InvalidStateTransitionException("No se puede editar una publicación que ya ha sido vendida");
        }
        if (status == PublicationStatus.RETIRED) {
            throw new InvalidStateTransitionException("No se puede editar una publicación que ha sido retirada");
        }
        if (status == PublicationStatus.DONATED) {
            throw new InvalidStateTransitionException("No se puede editar una prenda que ya fue donada");
        }

        this.garment = newGarment;
        this.size = newSize;
        this.conservationStatus = newConservationStatus;
        this.price = newPrice;
        this.photos = newPhotos != null ? new ArrayList<>(newPhotos) : new ArrayList<>();

        validateInvariants();
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Marca la publicación como vendida en el marketplace de segunda mano.
     */
    public void markAsSold() {
        if (status == PublicationStatus.SOLD) {
            throw new InvalidStateTransitionException("La publicación ya se encuentra marcada como vendida");
        }
        if (status == PublicationStatus.RETIRED) {
            throw new InvalidStateTransitionException("No se puede marcar como vendida una publicación retirada");
        }
        if (status == PublicationStatus.IN_DONATION || status == PublicationStatus.DONATED) {
            throw new InvalidStateTransitionException("No se puede vender una prenda destinada a donación");
        }

        this.status = PublicationStatus.SOLD;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Retira la publicación del catálogo público.
     */
    public void retire() {
        if (status == PublicationStatus.RETIRED) {
            throw new InvalidStateTransitionException("La publicación ya se encuentra retirada");
        }
        if (status == PublicationStatus.DONATED) {
            throw new InvalidStateTransitionException("No se puede retirar una prenda que ya fue donada");
        }

        this.status = PublicationStatus.RETIRED;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Destina la prenda hacia el canal benéfico de donaciones a fundaciones aliadas.
     */
    public void markForDonation() {
        if (status != PublicationStatus.AVAILABLE) {
            throw new InvalidStateTransitionException("Solo una publicación disponible puede destinarse a donación");
        }

        this.status = PublicationStatus.IN_DONATION;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Concluye el ciclo benéfico marcando la prenda como donada a la fundación.
     */
    public void markAsDonated() {
        if (status != PublicationStatus.IN_DONATION) {
            throw new InvalidStateTransitionException("Solo una prenda marcada en donación puede pasar a estado donada");
        }

        this.status = PublicationStatus.DONATED;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}

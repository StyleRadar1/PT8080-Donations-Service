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
 * Agregado raíz que representa una publicación en el módulo de Economía Circular de StyleRadar.
 * Unifica:
 * 1) Prendas de Donación benéfica (esDonacion = true, precio = 0 COP, vitrina abierta a fundaciones).
 * 2) Prendas de Segunda Mano C2C (esDonacion = false, precio >= 0 COP, chat entre usuarios).
 * 
 * Restricción geográfica: Opera exclusivamente en las 20 localidades de Bogotá D.C.
 * Concurrencia: Protegido mediante versión para bloqueo optimista (@Version).
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

    @Builder.Default
    private boolean esDonacion = false;

    @Builder.Default
    private LocalidadBogota localidad = LocalidadBogota.CHAPINERO;

    private PublicationStatus status;
    private Long version;
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

        if (localidad == null) {
            this.localidad = LocalidadBogota.CHAPINERO;
        }

        if (esDonacion) {
            if (price == null) {
                this.price = BigDecimal.ZERO;
            } else if (price.compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidPublicationException("Una prenda destinada a donación benéfica debe tener precio 0 COP");
            }
        } else {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidPublicationException("Para publicaciones de segunda mano (venta C2C), el precio debe ser estrictamente mayor a 0 COP");
            }
        }

        if (photos != null && photos.size() > MAX_PHOTOS) {
            throw new InvalidPublicationException("Una publicación no puede contener más de " + MAX_PHOTOS + " fotografías");
        }
    }

    /**
     * Publica la prenda haciéndola disponible en la vitrina pública.
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
                     BigDecimal newPrice, LocalidadBogota newLocalidad, List<String> newPhotos) {
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
        if (this.esDonacion) {
            this.price = BigDecimal.ZERO;
        } else {
            this.price = newPrice;
        }
        if (newLocalidad != null) {
            this.localidad = newLocalidad;
        }
        this.photos = newPhotos != null ? new ArrayList<>(newPhotos) : new ArrayList<>();

        validateInvariants();
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Sobrecarga de compatibilidad para edición básica.
     */
    public void edit(Garment newGarment, Size newSize, ConservationStatus newConservationStatus,
                     BigDecimal newPrice, List<String> newPhotos) {
        edit(newGarment, newSize, newConservationStatus, newPrice, this.localidad, newPhotos);
    }

    /**
     * Marca la publicación como vendida en el marketplace de segunda mano C2C.
     */
    public void markAsSold() {
        if (esDonacion) {
            throw new InvalidStateTransitionException("Una prenda publicada para donación no puede ser vendida");
        }
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
            throw new InvalidStateTransitionException(
                    "Solo una publicación disponible en vitrina puede destinarse a donación. Estado actual: " + status);
        }

        this.status = PublicationStatus.IN_DONATION;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Concluye el ciclo benéfico marcando la prenda como donada a la fundación receptora.
     */
    public void markAsDonated() {
        if (status != PublicationStatus.IN_DONATION) {
            throw new InvalidStateTransitionException(
                    "Solo una prenda marcada en donación puede pasar a estado donada. Estado actual: " + status);
        }

        this.status = PublicationStatus.DONATED;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}

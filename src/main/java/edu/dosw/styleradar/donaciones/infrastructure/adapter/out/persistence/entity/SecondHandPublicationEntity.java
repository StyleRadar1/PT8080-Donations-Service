package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.PublicationStatus;
import edu.dosw.styleradar.donaciones.domain.model.Size;
import edu.dosw.styleradar.donaciones.domain.model.Style;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA para la tabla publicaciones_segunda_mano.
 * Representa publicaciones de donaciones (esDonacion = true) y segunda mano (esDonacion = false).
 */
@Entity
@Table(name = "publicaciones_segunda_mano")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondHandPublicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long userId;

    @Column(name = "nombre_prenda", nullable = false, length = 120)
    private String garmentName;

    @Column(name = "descripcion", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_prenda", nullable = false, length = 30)
    private GarmentType garmentType;

    @Column(name = "marca", nullable = false, length = 80)
    private String brand;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "estilo", nullable = false, length = 30)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "talla", nullable = false, length = 20)
    private Size size;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacion", nullable = false, length = 30)
    private ConservationStatus conservationStatus;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "es_donacion", nullable = false)
    @Builder.Default
    private boolean esDonacion = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "localidad", nullable = false, length = 50)
    @Builder.Default
    private LocalidadBogota localidad = LocalidadBogota.CHAPINERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private PublicationStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<PublicationPhotoEntity> photos = new ArrayList<>();

    public void addPhoto(String photoUrl, int orderIndex) {
        PublicationPhotoEntity photo = PublicationPhotoEntity.builder()
                .publication(this)
                .photoUrl(photoUrl)
                .orderIndex(orderIndex)
                .build();
        photos.add(photo);
    }
}

package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA para la tabla fotos_publicacion (máximo 4 fotografías por prenda).
 */
@Entity
@Table(name = "fotos_publicacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private SecondHandPublicationEntity publication;

    @Column(name = "url_foto", nullable = false, length = 512)
    private String photoUrl;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}

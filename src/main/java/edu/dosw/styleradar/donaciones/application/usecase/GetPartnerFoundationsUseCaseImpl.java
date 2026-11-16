package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidDonationRequestException;
import edu.dosw.styleradar.donaciones.domain.exception.ResourceNotFoundException;
import edu.dosw.styleradar.donaciones.domain.model.FoundationSummary;
import edu.dosw.styleradar.donaciones.domain.port.in.GetPartnerFoundationsUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementación del caso de uso para consultar las fundaciones aliadas activas desde ms-tiendas (SR-RF-44).
 */
@Service
@RequiredArgsConstructor
public class GetPartnerFoundationsUseCaseImpl implements GetPartnerFoundationsUseCase {

    private static final Map<String, String> CATEGORY_ALIASES = Map.of(
            "UPPER", "SUPERIOR",
            "LOWER", "INFERIOR",
            "DRESS", "SUPERIOR",
            "FOOTWEAR", "CALZADO",
            "ACCESSORY", "ACCESORIOS"
    );

    private final FoundationCatalogPort foundationCatalogPort;

    @Override
    public List<FoundationSummary> getActiveFoundations(String category) {
        String normalizedCategory = normalizeCategory(category);

        return foundationCatalogPort.findAllActiveFoundations().stream()
                .filter(FoundationSummary::activa)
                .filter(foundation -> matchesCategory(foundation, normalizedCategory))
                .sorted(Comparator.comparing(
                        foundation -> foundation.nombre() == null ? "" : foundation.nombre().toLowerCase(Locale.ROOT)
                ))
                .toList();
    }

    @Override
    public FoundationSummary getActiveFoundationById(Long foundationId) {
        if (foundationId == null) {
            throw new InvalidDonationRequestException("El ID de la fundación es obligatorio");
        }

        FoundationSummary foundation = foundationCatalogPort.findFoundationById(foundationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una fundación aliada registrada con ID: " + foundationId));

        if (!foundation.activa()) {
            throw new InvalidDonationRequestException(
                    "La fundación '" + foundation.nombre() + "' no se encuentra activa para recibir donaciones");
        }

        return foundation;
    }

    @Override
    public FoundationSummary getFoundationByRepresentativeId(Long representativeUserId) {
        if (representativeUserId == null) {
            throw new InvalidDonationRequestException("El ID del representante de la fundación es obligatorio");
        }

        return foundationCatalogPort.findAllActiveFoundations().stream()
                .filter(FoundationSummary::activa)
                .filter(foundation -> representativeUserId.equals(foundation.representanteId())
                        || representativeUserId.equals(foundation.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una fundación aliada activa asociada al usuario ID: " + representativeUserId));
    }

    private String normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        String upper = rawCategory.trim().toUpperCase(Locale.ROOT);
        return CATEGORY_ALIASES.getOrDefault(upper, upper);
    }

    private boolean matchesCategory(FoundationSummary foundation, String normalizedCategory) {
        if (normalizedCategory == null) {
            return true;
        }
        return foundation.categoriasAceptadas().stream()
                .anyMatch(cat -> cat != null && cat.trim().equalsIgnoreCase(normalizedCategory));
    }
}

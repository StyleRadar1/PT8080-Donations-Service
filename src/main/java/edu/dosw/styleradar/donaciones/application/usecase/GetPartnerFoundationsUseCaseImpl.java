package edu.dosw.styleradar.donaciones.application.usecase;

import java.text.Normalizer;
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

    private static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("UPPER", "SUPERIOR"),
            Map.entry("CAMISAS", "SUPERIOR"),
            Map.entry("CAMISA", "SUPERIOR"),
            Map.entry("CAMISETAS", "SUPERIOR"),
            Map.entry("CAMISETA", "SUPERIOR"),
            Map.entry("SUETERES", "SUPERIOR"),
            Map.entry("SUETER", "SUPERIOR"),
            Map.entry("CHAQUETAS", "SUPERIOR"),
            Map.entry("CHAQUETA", "SUPERIOR"),
            Map.entry("VESTIDOS", "SUPERIOR"),
            Map.entry("VESTIDO", "SUPERIOR"),
            Map.entry("DRESS", "SUPERIOR"),
            Map.entry("FULL_BODY", "SUPERIOR"),
            Map.entry("LOWER", "INFERIOR"),
            Map.entry("PANTALONES", "INFERIOR"),
            Map.entry("PANTALON", "INFERIOR"),
            Map.entry("FALDAS", "INFERIOR"),
            Map.entry("FALDA", "INFERIOR"),
            Map.entry("FOOTWEAR", "CALZADO"),
            Map.entry("CALZADO", "CALZADO"),
            Map.entry("ACCESSORY", "ACCESORIOS"),
            Map.entry("ACCESORIOS", "ACCESORIOS"),
            Map.entry("ACCESORIO", "ACCESORIOS")
    );

    private final FoundationCatalogPort foundationCatalogPort;

    @Override
    public List<FoundationSummary> getActiveFoundations(String category) {
        String rawNormalized = normalizeText(category);
        String groupCategory = rawNormalized != null
                ? CATEGORY_ALIASES.getOrDefault(rawNormalized, rawNormalized)
                : null;

        return foundationCatalogPort.findAllActiveFoundations().stream()
                .filter(FoundationSummary::activa)
                .filter(foundation -> matchesCategory(foundation, rawNormalized, groupCategory))
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
                .filter(foundation -> representativeUserId.equals(foundation.representanteId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una fundación aliada activa asociada al usuario ID: " + representativeUserId));
    }

    private String normalizeText(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        return Normalizer.normalize(rawCategory.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private boolean matchesCategory(FoundationSummary foundation, String rawNormalized, String groupCategory) {
        if (rawNormalized == null) {
            return true;
        }
        return foundation.categoriasAceptadas().stream()
                .map(this::normalizeText)
                .anyMatch(cat -> cat != null && (cat.equals(rawNormalized) || cat.equals(groupCategory)));
    }
}

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

    private static final String CATEGORY_SUPERIOR = "SUPERIOR";
    private static final String CATEGORY_INFERIOR = "INFERIOR";
    private static final String CATEGORY_CALZADO = "CALZADO";
    private static final String CATEGORY_ACCESORIOS = "ACCESORIOS";
    private static final String CATEGORY_VESTIDOS = "VESTIDOS";

    private static final Map<String, String> GROUP_ALIASES = Map.ofEntries(
            Map.entry(CATEGORY_SUPERIOR, CATEGORY_SUPERIOR),
            Map.entry("UPPER", CATEGORY_SUPERIOR),
            Map.entry("TOP", CATEGORY_SUPERIOR),
            Map.entry("FULL_BODY", CATEGORY_SUPERIOR),
            Map.entry(CATEGORY_INFERIOR, CATEGORY_INFERIOR),
            Map.entry("LOWER", CATEGORY_INFERIOR),
            Map.entry("BOTTOM", CATEGORY_INFERIOR),
            Map.entry(CATEGORY_CALZADO, CATEGORY_CALZADO),
            Map.entry("FOOTWEAR", CATEGORY_CALZADO),
            Map.entry(CATEGORY_ACCESORIOS, CATEGORY_ACCESORIOS),
            Map.entry("ACCESORIO", CATEGORY_ACCESORIOS),
            Map.entry("ACCESSORY", CATEGORY_ACCESORIOS),
            Map.entry("ACCESSORIES", CATEGORY_ACCESORIOS)
    );

    private static final Map<String, String> SPECIFIC_ALIASES = Map.ofEntries(
            Map.entry("CAMISA", "CAMISAS"),
            Map.entry("CAMISETA", "CAMISETAS"),
            Map.entry("SUETER", "SUETERES"),
            Map.entry("CHAQUETA", "CHAQUETAS"),
            Map.entry("PANTALON", "PANTALONES"),
            Map.entry("FALDA", "FALDAS"),
            Map.entry("VESTIDO", CATEGORY_VESTIDOS),
            Map.entry("DRESS", CATEGORY_VESTIDOS)
    );

    private static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("UPPER", CATEGORY_SUPERIOR),
            Map.entry("TOP", CATEGORY_SUPERIOR),
            Map.entry(CATEGORY_SUPERIOR, CATEGORY_SUPERIOR),
            Map.entry("CAMISAS", CATEGORY_SUPERIOR),
            Map.entry("CAMISA", CATEGORY_SUPERIOR),
            Map.entry("CAMISETAS", CATEGORY_SUPERIOR),
            Map.entry("CAMISETA", CATEGORY_SUPERIOR),
            Map.entry("SUETERES", CATEGORY_SUPERIOR),
            Map.entry("SUETER", CATEGORY_SUPERIOR),
            Map.entry("CHAQUETAS", CATEGORY_SUPERIOR),
            Map.entry("CHAQUETA", CATEGORY_SUPERIOR),
            Map.entry(CATEGORY_VESTIDOS, CATEGORY_SUPERIOR),
            Map.entry("VESTIDO", CATEGORY_SUPERIOR),
            Map.entry("DRESS", CATEGORY_SUPERIOR),
            Map.entry("FULL_BODY", CATEGORY_SUPERIOR),
            Map.entry("LOWER", CATEGORY_INFERIOR),
            Map.entry("BOTTOM", CATEGORY_INFERIOR),
            Map.entry(CATEGORY_INFERIOR, CATEGORY_INFERIOR),
            Map.entry("PANTALONES", CATEGORY_INFERIOR),
            Map.entry("PANTALON", CATEGORY_INFERIOR),
            Map.entry("FALDAS", CATEGORY_INFERIOR),
            Map.entry("FALDA", CATEGORY_INFERIOR),
            Map.entry("FOOTWEAR", CATEGORY_CALZADO),
            Map.entry(CATEGORY_CALZADO, CATEGORY_CALZADO),
            Map.entry("ACCESSORY", CATEGORY_ACCESORIOS),
            Map.entry("ACCESSORIES", CATEGORY_ACCESORIOS),
            Map.entry(CATEGORY_ACCESORIOS, CATEGORY_ACCESORIOS),
            Map.entry("ACCESORIO", CATEGORY_ACCESORIOS)
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

        FoundationSummary foundation = foundationCatalogPort.findFoundationByRepresentativeId(representativeUserId)
                .filter(FoundationSummary::activa)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una fundación aliada activa asociada al usuario ID: " + representativeUserId));

        if (foundation.representanteId() == null || !representativeUserId.equals(foundation.representanteId())) {
            throw new ResourceNotFoundException(
                    "No se encontró una fundación aliada activa asociada al usuario ID: " + representativeUserId);
        }

        return foundation;
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
        if (foundation.categoriasAceptadas() == null) {
            return false;
        }
        String querySpecific = SPECIFIC_ALIASES.getOrDefault(rawNormalized, rawNormalized);
        return foundation.categoriasAceptadas().stream()
                .map(this::normalizeText)
                .anyMatch(cat -> {
                    if (cat == null) {
                        return false;
                    }
                    String foundationSpecific = SPECIFIC_ALIASES.getOrDefault(cat, cat);
                    if (foundationSpecific.equals(querySpecific)) {
                        return true;
                    }
                    // Solo expandimos a grupo si la fundación declara explícitamente una categoría grupal
                    // (e.g., TOP/BOTTOM/FOOTWEAR/SUPERIOR/INFERIOR), respetando tipos específicos (e.g., FALDAS).
                    String foundationGroup = GROUP_ALIASES.get(cat);
                    return foundationGroup != null && foundationGroup.equals(groupCategory);
                });
    }
}

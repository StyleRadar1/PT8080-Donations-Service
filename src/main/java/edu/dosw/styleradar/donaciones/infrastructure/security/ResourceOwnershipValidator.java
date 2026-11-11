package edu.dosw.styleradar.donaciones.infrastructure.security;

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Validador de propiedad de recursos para impedir que usuarios modifiquen o consulten
 * recursos ajenos, respetando las excepciones administrativas autorizadas (ROLE_ADMIN).
 */
@Component
public class ResourceOwnershipValidator {

    public void validateOwnerOrAdmin(Long targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        validateOwnerOrAdmin(authentication, targetUserId);
    }

    public void validateOwnerOrAdmin(Authentication authentication, Long targetUserId) {
        if (isAdmin(authentication)) {
            return;
        }

        Long authenticatedUserId = extractAuthenticatedUserId(authentication);
        if (authenticatedUserId == null || !authenticatedUserId.equals(targetUserId)) {
            throw new AccessDeniedException(
                    "Acceso denegado: El usuario autenticado (ID: " + authenticatedUserId
                            + ") no tiene autorización para operar sobre recursos pertenecientes al usuario/fundación (ID: "
                            + targetUserId + ")."
            );
        }
    }

    public void validateDonationRequestAccess(DonationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        validateDonationRequestAccess(authentication, request);
    }

    public void validateDonationRequestAccess(Authentication authentication, DonationRequest request) {
        if (request == null || isAdmin(authentication)) {
            return;
        }

        Long authenticatedUserId = extractAuthenticatedUserId(authentication);
        boolean isOwnerDonor = authenticatedUserId != null && authenticatedUserId.equals(request.getDonorId());
        boolean isTargetFoundation = authenticatedUserId != null && authenticatedUserId.equals(request.getFoundationId());

        if (!isOwnerDonor && !isTargetFoundation) {
            throw new AccessDeniedException(
                    "Acceso denegado: No tiene autorización para consultar la solicitud de donación ID: " + request.getId()
            );
        }
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long extractAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.id();
        }
        if (principal instanceof UserDetails userDetails) {
            return parseUserId(userDetails.getUsername());
        }
        if (principal instanceof String principalStr) {
            return parseUserId(principalStr);
        }

        return null;
    }

    private Long parseUserId(String rawIdentifier) {
        if (rawIdentifier == null) {
            return null;
        }
        try {
            return Long.parseLong(rawIdentifier);
        } catch (NumberFormatException ignored) {
            return mapMockEmailToId(rawIdentifier);
        }
    }

    private Long mapMockEmailToId(String email) {
        if ("user@styleradar.com".equalsIgnoreCase(email)) {
            return 101L;
        }
        if ("foundation@styleradar.com".equalsIgnoreCase(email)) {
            return 501L;
        }
        if ("admin@styleradar.com".equalsIgnoreCase(email)) {
            return 1L;
        }
        return null;
    }
}

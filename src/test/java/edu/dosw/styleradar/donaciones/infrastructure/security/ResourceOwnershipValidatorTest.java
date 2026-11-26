package edu.dosw.styleradar.donaciones.infrastructure.security;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import edu.dosw.styleradar.donaciones.domain.exception.ExternalServiceUnavailableException;
import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;
import edu.dosw.styleradar.donaciones.domain.port.out.FoundationCatalogPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceOwnershipValidatorTest {

    private final ResourceOwnershipValidator validator = new ResourceOwnershipValidator();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("validateOwnerOrAdmin debe permitir acceso cuando el usuario es ROLE_ADMIN")
    void shouldAllowWhenAdmin() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(1L, "admin@styleradar.com", "pass", "Admin", Role.ADMIN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        assertThatCode(() -> validator.validateOwnerOrAdmin(999L)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateOwnerOrAdmin(adminAuth, 999L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateOwnerOrAdmin debe permitir acceso cuando el ID del usuario coincide con targetUserId")
    void shouldAllowWhenOwnerMatches() {
        UserPrincipal principal = new UserPrincipal(101L, "user@styleradar.com", "pass", "User", Role.USER);
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(userAuth);

        assertThatCode(() -> validator.validateOwnerOrAdmin(101L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateOwnerOrAdmin debe lanzar AccessDeniedException cuando el ID del usuario no coincide")
    void shouldThrowWhenOwnerMismatches() {
        UserPrincipal principal = new UserPrincipal(101L, "user@styleradar.com", "pass", "User", Role.USER);
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(userAuth);

        assertThatThrownBy(() -> validator.validateOwnerOrAdmin(999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso denegado");
    }

    @Test
    @DisplayName("validateOwnerOrAdmin debe lanzar AccessDeniedException cuando la autenticación es nula")
    void shouldThrowWhenAuthenticationIsNull() {
        assertThatThrownBy(() -> validator.validateOwnerOrAdmin(null, 101L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe permitir acceso si request es nula o el usuario es ADMIN")
    void shouldAllowDonationRequestAccessForAdminOrNull() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(1L, "admin@styleradar.com", "pass", "Admin", Role.ADMIN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        assertThatCode(() -> validator.validateDonationRequestAccess(adminAuth, null)).doesNotThrowAnyException();

        DonationRequest request = DonationRequest.builder().id(1L).donorId(101L).foundationId(501L).build();
        assertThatCode(() -> validator.validateDonationRequestAccess(adminAuth, request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe permitir acceso al donante propietario")
    void shouldAllowDonationRequestAccessForDonor() {
        Authentication donorAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(101L, "user@styleradar.com", "pass", "User", Role.USER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(donorAuth);

        DonationRequest request = DonationRequest.builder().id(1L).donorId(101L).foundationId(501L).build();
        assertThatCode(() -> validator.validateDonationRequestAccess(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe permitir acceso solo al representanteId autorizado de la fundación destinataria")
    void shouldAllowDonationRequestAccessForFoundation() {
        FoundationCatalogPort foundationCatalogPort = mock(FoundationCatalogPort.class);
        ResourceOwnershipValidator representativeValidator = new ResourceOwnershipValidator(foundationCatalogPort);

        Authentication foundationAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(501L, "foundation@styleradar.com", "pass", "Foundation", Role.FOUNDATION),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION"))
        );
        SecurityContextHolder.getContext().setAuthentication(foundationAuth);
        when(foundationCatalogPort.isFoundationRepresentative(501L, 3L)).thenReturn(true);

        DonationRequest request = DonationRequest.builder().id(1L).donorId(101L).foundationId(3L).build();
        assertThatCode(() -> representativeValidator.validateDonationRequestAccess(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe denegar acceso aunque userId coincida numéricamente con foundationId sin ser representanteId")
    void shouldDenyDonationRequestAccessWhenUserIdOnlyMatchesFoundationIdNumerically() {
        FoundationCatalogPort foundationCatalogPort = mock(FoundationCatalogPort.class);
        ResourceOwnershipValidator representativeValidator = new ResourceOwnershipValidator(foundationCatalogPort);

        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(501L, "impostor@styleradar.com", "pass", "Impostor", Role.FOUNDATION),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION"))
        );
        SecurityContextHolder.getContext().setAuthentication(userAuth);
        when(foundationCatalogPort.isFoundationRepresentative(501L, 501L)).thenReturn(false);

        DonationRequest collidingRequest = DonationRequest.builder().id(1L).donorId(101L).foundationId(501L).build();
        assertThatThrownBy(() -> representativeValidator.validateDonationRequestAccess(collidingRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso denegado");

        assertThatThrownBy(() -> validator.validateDonationRequestAccess(collidingRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("validateFoundationOwnership debe denegar coincidencia numérica userId == foundationId y sin catálogo")
    void shouldDenyFoundationOwnershipOnNumericIdCollisionWithoutRepresentation() {
        FoundationCatalogPort foundationCatalogPort = mock(FoundationCatalogPort.class);
        ResourceOwnershipValidator representativeValidator = new ResourceOwnershipValidator(foundationCatalogPort);

        Authentication collidingAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(501L, "impostor@styleradar.com", "pass", "Impostor", Role.FOUNDATION),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION"))
        );
        SecurityContextHolder.getContext().setAuthentication(collidingAuth);
        when(foundationCatalogPort.isFoundationRepresentative(501L, 501L)).thenReturn(false);

        assertThatThrownBy(() -> representativeValidator.validateFoundationOwnership(501L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no tiene autorización");

        assertThatThrownBy(() -> validator.validateFoundationOwnership(501L))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> representativeValidator.validateFoundationOwnership(null, 501L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("validateFoundationOwnership debe permitir acceso a ROLE_ADMIN incluso sin catálogo de fundaciones")
    void shouldAllowFoundationOwnershipForAdmin() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(1L, "admin@styleradar.com", "pass", "Admin", Role.ADMIN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        assertThatCode(() -> validator.validateFoundationOwnership(3L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe lanzar AccessDeniedException para un tercero no relacionado")
    void shouldDenyDonationRequestAccessForThirdParty() {
        Authentication thirdPartyAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(202L, "other@styleradar.com", "pass", "Other", Role.USER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(thirdPartyAuth);

        DonationRequest request = DonationRequest.builder().id(1L).donorId(101L).foundationId(501L).build();
        assertThatThrownBy(() -> validator.validateDonationRequestAccess(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso denegado");
    }

    @Test
    @DisplayName("isAdmin debe retornar false para autenticación nula o no autenticada")
    void shouldReturnFalseForNullOrUnauthenticatedAdminCheck() {
        assertThat(validator.isAdmin(null)).isFalse();

        Authentication unauthenticated = new UsernamePasswordAuthenticationToken("anon", "pass");
        // por defecto es unauthenticated cuando se usa constructor de 2 args
        assertThat(validator.isAdmin(unauthenticated)).isFalse();
    }

    @Test
    @DisplayName("extractAuthenticatedUserId debe extraer ID desde UserPrincipal, UserDetails y String")
    void shouldExtractUserIdFromDifferentPrincipalTypes() {
        // UserPrincipal
        UserPrincipal principal = new UserPrincipal(101L, "user@styleradar.com", "pass", "User", Role.USER);
        Authentication auth1 = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(validator.extractAuthenticatedUserId(auth1)).isEqualTo(101L);

        // UserDetails con ID numérico
        UserDetails userDetailsNumeric = new User("102", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth2 = new UsernamePasswordAuthenticationToken(userDetailsNumeric, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(validator.extractAuthenticatedUserId(auth2)).isEqualTo(102L);

        // UserDetails con correos conocidos
        UserDetails userDetailsUser = new User("user@styleradar.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken(userDetailsUser, null, List.of()))).isEqualTo(101L);

        UserDetails userDetailsFoundation = new User("foundation@styleradar.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION")));
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken(userDetailsFoundation, null, List.of()))).isEqualTo(501L);

        UserDetails userDetailsAdmin = new User("admin@styleradar.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken(userDetailsAdmin, null, List.of()))).isEqualTo(1L);

        UserDetails userDetailsUnknown = new User("unknown@domain.com", "pass", List.of());
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken(userDetailsUnknown, null, List.of()))).isNull();

        // String principal numérico
        Authentication authStringNum = new UsernamePasswordAuthenticationToken("205", null, List.of());
        assertThat(validator.extractAuthenticatedUserId(authStringNum)).isEqualTo(205L);

        // String principal correos conocidos
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken("user@styleradar.com", null, List.of()))).isEqualTo(101L);
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken("foundation@styleradar.com", null, List.of()))).isEqualTo(501L);
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken("admin@styleradar.com", null, List.of()))).isEqualTo(1L);
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken("unknown", null, List.of()))).isNull();

        // Tipo desconocido de principal
        assertThat(validator.extractAuthenticatedUserId(new UsernamePasswordAuthenticationToken(new Object(), null, List.of()))).isNull();

        // Auth nulo
        assertThat(validator.extractAuthenticatedUserId(null)).isNull();
    }

    @Test
    @DisplayName("validateFoundationOwnership y validateDonationRequestAccess deben autorizar al representanteId registrado en ms-tiendas")
    void shouldAllowFoundationAccessViaRepresentativeIdFromTiendas() {
        FoundationCatalogPort foundationCatalogPort = mock(FoundationCatalogPort.class);
        ResourceOwnershipValidator representativeValidator = new ResourceOwnershipValidator(foundationCatalogPort);

        Authentication repAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(24L, "user-24@styleradar.local", "", "Usuario #24", Role.FOUNDATION),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION"))
        );
        SecurityContextHolder.getContext().setAuthentication(repAuth);

        when(foundationCatalogPort.isFoundationRepresentative(24L, 3L)).thenReturn(true);
        when(foundationCatalogPort.isFoundationRepresentative(24L, 99L)).thenReturn(false);

        assertThatCode(() -> representativeValidator.validateFoundationOwnership(3L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> representativeValidator.validateFoundationOwnership(99L))
                .isInstanceOf(AccessDeniedException.class);

        DonationRequest reqAllowed = DonationRequest.builder().id(10L).donorId(101L).foundationId(3L).build();
        assertThatCode(() -> representativeValidator.validateDonationRequestAccess(reqAllowed))
                .doesNotThrowAnyException();

        DonationRequest reqForbidden = DonationRequest.builder().id(11L).donorId(101L).foundationId(99L).build();
        assertThatThrownBy(() -> representativeValidator.validateDonationRequestAccess(reqForbidden))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("validateDonationRequestAccess debe retornar de inmediato para el donante propietario y denegar con 403 al donante ajeno sin consultar ms-tiendas aunque esté indisponible")
    void shouldBypassTiendasForOwnerDonorAndForeignDonorEvenWhenTiendasUnavailable() {
        FoundationCatalogPort foundationCatalogPort = mock(FoundationCatalogPort.class);
        when(foundationCatalogPort.isFoundationRepresentative(any(), any()))
                .thenThrow(new ExternalServiceUnavailableException("ms-tiendas indisponible"));
        ResourceOwnershipValidator representativeValidator = new ResourceOwnershipValidator(foundationCatalogPort);

        DonationRequest request = DonationRequest.builder().id(10L).donorId(101L).foundationId(3L).build();

        // Donante propietario (USER): retorna sin consultar ms-tiendas
        Authentication ownerDonorAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(101L, "donor@styleradar.com", "pass", "Donor", Role.USER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        assertThatCode(() -> representativeValidator.validateDonationRequestAccess(ownerDonorAuth, request))
                .doesNotThrowAnyException();

        // Donante ajeno (USER): lanza AccessDeniedException sin consultar ms-tiendas
        Authentication foreignDonorAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(202L, "foreign@styleradar.com", "pass", "Foreign", Role.USER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        assertThatThrownBy(() -> representativeValidator.validateDonationRequestAccess(foreignDonorAuth, request))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(foundationCatalogPort);
    }
}

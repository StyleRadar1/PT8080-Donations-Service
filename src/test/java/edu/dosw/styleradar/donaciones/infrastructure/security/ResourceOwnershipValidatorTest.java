package edu.dosw.styleradar.donaciones.infrastructure.security;

import java.util.Collections;
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

import edu.dosw.styleradar.donaciones.domain.model.DonationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("validateDonationRequestAccess debe permitir acceso a la fundación destinataria")
    void shouldAllowDonationRequestAccessForFoundation() {
        Authentication foundationAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(501L, "foundation@styleradar.com", "pass", "Foundation", Role.FOUNDATION),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FOUNDATION"))
        );
        SecurityContextHolder.getContext().setAuthentication(foundationAuth);

        DonationRequest request = DonationRequest.builder().id(1L).donorId(101L).foundationId(501L).build();
        assertThatCode(() -> validator.validateDonationRequestAccess(request)).doesNotThrowAnyException();
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
}

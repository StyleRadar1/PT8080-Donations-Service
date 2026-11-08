package edu.dosw.styleradar.donaciones.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Inicializa con una clave de prueba de 256 bits codificada en base64 y duración de 1 hora
        String testSecret = "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";
        jwtTokenProvider = new JwtTokenProvider(testSecret, Duration.ofHours(1));
    }

    @Test
    @DisplayName("Debe generar un token válido y extraer correctamente sus claims")
    void shouldGenerateAndExtractClaims() {
        Long userId = 101L;
        String email = "user@styleradar.com";
        Role role = Role.USER;
        String name = "Juan Pérez";

        String token = jwtTokenProvider.generateToken(userId, email, role, name);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(role);
        assertThat(jwtTokenProvider.getName(token)).isEqualTo(name);
        assertThat(jwtTokenProvider.getExpiresInSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Debe rechazar un token malformado o adulterado")
    void shouldRejectInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("token.invalido.malformado")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Debe manejar clave nula o vacía usando la clave predeterminada de 256 bits")
    void shouldHandleDefaultKey() {
        JwtTokenProvider defaultProvider = new JwtTokenProvider(null, null);
        String token = defaultProvider.generateToken(1L, "admin@styleradar.com", Role.ADMIN, "Admin");

        assertThat(defaultProvider.validateToken(token)).isTrue();
        assertThat(defaultProvider.getRole(token)).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Debe soportar clave de texto plano corta mediante padding a 256 bits")
    void shouldHandleShortKeyWithPadding() {
        JwtTokenProvider shortKeyProvider = new JwtTokenProvider("clave-corta", Duration.ofMinutes(10));
        String token = shortKeyProvider.generateToken(501L, "foundation@styleradar.com", Role.FOUNDATION, "Fundación");

        assertThat(shortKeyProvider.validateToken(token)).isTrue();
        assertThat(shortKeyProvider.getUserId(token)).isEqualTo(501L);
    }
}

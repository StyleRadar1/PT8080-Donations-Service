package edu.dosw.styleradar.donaciones.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String testSecret = "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        jwtTokenProvider = new JwtTokenProvider(testSecret, Duration.ofHours(1), env);
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
    @DisplayName("En entorno local/test debe permitir clave predeterminada de 256 bits si no se suministra una externa")
    void shouldHandleDefaultKeyInLocalOrTest() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        JwtTokenProvider defaultProvider = new JwtTokenProvider(null, null, env);
        String token = defaultProvider.generateToken(1L, "admin@styleradar.com", Role.ADMIN, "Admin");

        assertThat(defaultProvider.validateToken(token)).isTrue();
        assertThat(defaultProvider.getRole(token)).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("En ambiente productivo debe exigir estrictamente la variable JWT_SECRET_BASE64")
    void shouldEnforceSecretInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtTokenProvider(null, Duration.ofHours(1), prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");

        assertThatThrownBy(() -> new JwtTokenProvider("", Duration.ofHours(1), prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");

        assertThatThrownBy(() -> new JwtTokenProvider("clave-corta-invalida", Duration.ofHours(1), prodEnv))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Debe soportar clave de texto plano corta mediante padding en entornos de prueba")
    void shouldHandleShortKeyWithPaddingInTest() {
        MockEnvironment testEnv = new MockEnvironment();
        testEnv.setActiveProfiles("test");
        JwtTokenProvider shortKeyProvider = new JwtTokenProvider("clave-corta", Duration.ofMinutes(10), testEnv);
        String token = shortKeyProvider.generateToken(501L, "foundation@styleradar.com", Role.FOUNDATION, "Fundación");

        assertThat(shortKeyProvider.validateToken(token)).isTrue();
        assertThat(shortKeyProvider.getUserId(token)).isEqualTo(501L);
    }
}

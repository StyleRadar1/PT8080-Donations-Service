package edu.dosw.styleradar.donaciones.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

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
    @DisplayName("Debe validar exitosamente un token vigente con fecha de expiración futura")
    void shouldValidateValidTokenWithFutureExpiration() {
        String token = jwtTokenProvider.generateToken(201L, "valid@styleradar.com", Role.USER, "Valid User");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Debe rechazar un token expirado cuya fecha de expiración esté en el pasado")
    void shouldRejectExpiredToken() {
        String testSecret = "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(testSecret), "HmacSHA256");
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject("expired@styleradar.com")
                .claim("userId", 301L)
                .claim("role", Role.USER.name())
                .claim("name", "Expired User")
                .claim("iat", now.minusSeconds(120).getEpochSecond())
                .claim("exp", now.minusSeconds(60).getEpochSecond())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar un token que no contenga el claim de expiración 'exp'")
    void shouldRejectTokenWithoutExpClaim() {
        String testSecret = "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(testSecret), "HmacSHA256");
        Instant now = Instant.now();
        String tokenWithoutExp = Jwts.builder()
                .subject("noexp@styleradar.com")
                .claim("userId", 401L)
                .claim("role", Role.USER.name())
                .claim("name", "No Exp User")
                .claim("iat", now.getEpochSecond())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtTokenProvider.validateToken(tokenWithoutExp)).isFalse();
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
        Duration duration = Duration.ofHours(1);

        assertThatThrownBy(() -> new JwtTokenProvider(null, duration, prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");

        assertThatThrownBy(() -> new JwtTokenProvider("", duration, prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");

        assertThatThrownBy(() -> new JwtTokenProvider("clave-corta-invalida", duration, prodEnv))
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

    @Test
    @DisplayName("En perfil default o sin perfil activo debe exigir clave externa y rechazar clave predeterminada")
    void shouldEnforceSecretInDefaultProfile() {
        MockEnvironment defaultEnv = new MockEnvironment(); // Sin perfiles activos (default)
        Duration duration = Duration.ofHours(1);

        assertThatThrownBy(() -> new JwtTokenProvider(null, duration, defaultEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");

        // También con environment nulo
        assertThatThrownBy(() -> new JwtTokenProvider(null, duration, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET_BASE64 es obligatoria");
    }

    @Test
    @DisplayName("En perfil dev debe permitir clave predeterminada")
    void shouldAllowDefaultKeyInDevProfile() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        JwtTokenProvider devProvider = new JwtTokenProvider(null, null, devEnv);
        String token = devProvider.generateToken(101L, "dev@styleradar.com", Role.USER, "Dev User");

        assertThat(devProvider.validateToken(token)).isTrue();
        assertThat(devProvider.getRole(token)).isEqualTo(Role.USER);
    }
}

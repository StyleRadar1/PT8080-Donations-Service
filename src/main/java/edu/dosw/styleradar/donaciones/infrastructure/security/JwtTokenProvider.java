package edu.dosw.styleradar.donaciones.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Proveedor de tokens JWT para autenticación segura en la API y Swagger.
 * En perfiles productivos exige estrictamente una clave secreta segura (>= 256 bits) por variable de entorno.
 */
@Component
@SuppressWarnings({"java:S6418", "java:S2068"})
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    // Clave de desarrollo predeterminada para perfiles local/test exclusivamente
    private static final String DEFAULT_DEV_SECRET_BASE64 =
            "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";

    private final SecretKey key;
    private final Duration duration;
    private final JwtParser parser;

    public JwtTokenProvider(
            @Value("${styleradar.security.jwt.secret-base64:}") String secretBase64,
            @Value("${styleradar.security.jwt.duration:PT24H}") Duration duration,
            Environment env) {

        boolean isLocalOrTest = isLocalOrTestProfile(env);
        String effectiveSecret = resolveEffectiveSecret(secretBase64, env, isLocalOrTest);
        byte[] decodedKey = resolveKeyBytes(effectiveSecret, isLocalOrTest);

        this.key = new SecretKeySpec(decodedKey, "HmacSHA256");
        this.duration = resolveDuration(duration);
        this.parser = Jwts.parser().verifyWith(this.key).build();
    }

    private static String resolveEffectiveSecret(String secretBase64, Environment env, boolean isLocalOrTest) {
        String secret = secretBase64;
        if ((secret == null || secret.isBlank()) && env != null) {
            secret = env.getProperty("JWT_SECRET_BASE64");
            if (secret == null || secret.isBlank()) {
                secret = env.getProperty("SECURITY_JWT_SECRET");
            }
        }
        if (secret == null || secret.isBlank()) {
            if (isLocalOrTest) {
                log.warn("ADVERTENCIA DE SEGURIDAD: Usando clave secreta JWT predeterminada para entorno local/pruebas.");
                return DEFAULT_DEV_SECRET_BASE64;
            }
            throw new IllegalStateException(
                    "La variable de entorno JWT_SECRET_BASE64 es obligatoria y debe contener al menos 256 bits (32 bytes en Base64) en cualquier perfil distinto a local, test o dev."
            );
        }
        return secret;
    }

    private static byte[] resolveKeyBytes(String effectiveSecret, boolean isLocalOrTest) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(effectiveSecret);
            if (decodedKey.length >= 32) {
                return decodedKey;
            }
            if (isLocalOrTest) {
                return padTo256Bits(effectiveSecret.getBytes(StandardCharsets.UTF_8));
            }
            throw new IllegalStateException("JWT_SECRET_BASE64 es requerida y debe contener al menos 256 bits (32 bytes).");
        } catch (IllegalArgumentException e) {
            if (isLocalOrTest) {
                return padTo256Bits(effectiveSecret.getBytes(StandardCharsets.UTF_8));
            }
            throw new IllegalStateException("JWT_SECRET_BASE64 debe ser una cadena Base64 válida con al menos 256 bits.", e);
        }
    }

    private static Duration resolveDuration(Duration duration) {
        return (duration == null || duration.isNegative() || duration.isZero())
                ? Duration.ofHours(24)
                : duration;
    }

    private static boolean isLocalOrTestProfile(Environment env) {
        if (env == null) return false;
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        return activeProfiles.contains("local")
                || activeProfiles.contains("test")
                || activeProfiles.contains("dev");
    }

    private static byte[] padTo256Bits(byte[] input) {
        byte[] padded = new byte[32];
        System.arraycopy(input, 0, padded, 0, Math.min(input.length, 32));
        for (int i = input.length; i < 32; i++) {
            padded[i] = (byte) (i * 7);
        }
        return padded;
    }

    public String generateToken(Long userId, String email, Role role, String name) {
        Instant now = Instant.now();
        Instant expiry = now.plus(duration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .claim("name", name)
                .claim("iat", now.getEpochSecond())
                .claim("exp", expiry.getEpochSecond())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Object exp = claims.get("exp");
            if (exp == null) {
                return false;
            }
            long expSeconds = exp instanceof Number number ? number.longValue() : Long.parseLong(exp.toString());
            return expSeconds > Instant.now().getEpochSecond();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        Object userIdClaim = getClaims(token).get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(userIdClaim.toString());
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Role getRole(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public String getName(String token) {
        return getClaims(token).get("name", String.class);
    }

    public long getExpiresInSeconds() {
        return duration.toSeconds();
    }
}

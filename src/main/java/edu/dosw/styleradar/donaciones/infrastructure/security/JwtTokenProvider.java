package edu.dosw.styleradar.donaciones.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Proveedor de tokens JWT para autenticación segura en la API, compatible tanto con los tokens
 * oficiales emitidos por ms-auth (sub numérico + lista roles) como con los tokens demo de Swagger.
 * En perfiles productivos exige estrictamente una clave secreta segura (>= 256 bits) por variable de entorno.
 */
@Component
@SuppressWarnings({"java:S6418", "java:S2068"})
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    // Clave de desarrollo predeterminada para perfiles local/test exclusivamente
    private static final String DEFAULT_DEV_SECRET_BASE64 =
            "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";

    private static final String ROLE_PREFIX = "ROLE_";

    private static final Map<String, Role> ROLE_ALIASES = Map.ofEntries(
            Map.entry("USER", Role.USER),
            Map.entry("COMPRADOR", Role.USER),
            Map.entry("STORE", Role.STORE),
            Map.entry("ALMACEN", Role.STORE),
            Map.entry("TIENDA", Role.STORE),
            Map.entry("ADMIN_ALMACEN", Role.STORE),
            Map.entry("FOUNDATION", Role.FOUNDATION),
            Map.entry("FUNDACION", Role.FOUNDATION),
            Map.entry("REPRESENTANTE_FUNDACION", Role.FOUNDATION),
            Map.entry("ADMIN", Role.ADMIN),
            Map.entry("ADMIN_STYLERADAR", Role.ADMIN)
    );

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
        return env.acceptsProfiles(Profiles.of("local", "test", "dev"));
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
        Claims claims = getClaims(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim == null) {
            userIdClaim = claims.get("id");
        }
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim != null && !userIdClaim.toString().isBlank()) {
            return Long.valueOf(userIdClaim.toString().trim());
        }

        String subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
            return Long.valueOf(subject.trim());
        }
        throw new IllegalArgumentException("El token JWT no contiene un identificador de usuario válido.");
    }

    public String getEmail(String token) {
        Claims claims = getClaims(token);
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        String subject = claims.getSubject();
        if (subject != null && subject.contains("@")) {
            return subject.trim();
        }
        return "user-" + getUserId(token) + "@styleradar.local";
    }

    public Role getRole(String token) {
        Claims claims = getClaims(token);
        Object rawRoles = claims.get("roles");
        if (rawRoles == null) {
            rawRoles = claims.get("authorities");
        }
        if (rawRoles == null) {
            rawRoles = claims.get("role");
        }

        if (rawRoles instanceof Collection<?> collection) {
            for (Object item : collection) {
                Role resolved = resolveSingleRole(item != null ? item.toString() : null);
                if (resolved != null) {
                    return resolved;
                }
            }
        } else if (rawRoles instanceof String rawStr) {
            for (String tokenRole : rawStr.split(",")) {
                Role resolved = resolveSingleRole(tokenRole);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        throw new IllegalArgumentException("El token JWT no contiene un rol reconocido por StyleRadar: " + rawRoles);
    }

    public String getName(String token) {
        Claims claims = getClaims(token);
        String name = claims.get("name", String.class);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "Usuario #" + getUserId(token);
    }

    private static Role resolveSingleRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith(ROLE_PREFIX)) {
            normalized = normalized.substring(ROLE_PREFIX.length());
        }
        return ROLE_ALIASES.get(normalized);
    }

    public long getExpiresInSeconds() {
        return duration.toSeconds();
    }
}

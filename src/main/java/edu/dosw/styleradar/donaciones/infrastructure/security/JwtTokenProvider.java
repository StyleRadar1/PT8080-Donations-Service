package edu.dosw.styleradar.donaciones.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * Proveedor de tokens JWT para autenticación segura en la API y Swagger.
 */
@Component
public class JwtTokenProvider {

    // Clave secreta predeterminada de 256 bits si no se proporciona variable de entorno
    private static final String DEFAULT_SECRET_BASE64 =
            "c3R5bGVyYWRhci1zZWNyZXQta2V5LWZvci1kb3N3LTIwMjYtcHJvamVjdC1qd3Q=";

    private final SecretKey key;
    private final Duration duration;
    private final JwtParser parser;

    public JwtTokenProvider(
            @Value("${styleradar.security.jwt.secret-base64:}") String secretBase64,
            @Value("${styleradar.security.jwt.duration:PT24H}") Duration duration) {
        String effectiveSecret = (secretBase64 == null || secretBase64.isBlank())
                ? DEFAULT_SECRET_BASE64
                : secretBase64;

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(effectiveSecret);
            if (decodedKey.length < 32) {
                decodedKey = padTo256Bits(effectiveSecret.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException e) {
            decodedKey = padTo256Bits(effectiveSecret.getBytes(StandardCharsets.UTF_8));
        }

        this.key = new SecretKeySpec(decodedKey, "HmacSHA256");
        this.duration = (duration == null || duration.isNegative() || duration.isZero())
                ? Duration.ofHours(24)
                : duration;
        this.parser = Jwts.parser().verifyWith(this.key).build();
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
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + duration.toMillis());

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .claim("name", name)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
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

package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Successful authentication response containing the Bearer JWT token")
public record LoginResponseDTO(
        @Schema(description = "Bearer JWT token for authorizing Swagger and REST client requests")
        String token,

        @Schema(description = "Authentication token scheme", example = "Bearer")
        String type,

        @Schema(description = "Token expiration time in seconds", example = "86400")
        long expiresIn,

        @Schema(description = "User identifier", example = "101")
        Long userId,

        @Schema(description = "User email address", example = "user@styleradar.com")
        String email,

        @Schema(description = "User full name or display name", example = "Juan Pérez (Usuario Normal)")
        String name,

        @Schema(description = "Assigned system role", example = "USER")
        String role,

        @Schema(description = "Spring Security authorities list", example = "[\"ROLE_USER\"]")
        List<String> authorities
) {
}

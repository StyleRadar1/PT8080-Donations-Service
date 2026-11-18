package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demo login credentials to obtain a JWT token")
public record LoginRequestDTO(
        @Schema(description = "User email address", example = "user@styleradar.com")
        @NotBlank(message = "El email es obligatorio")
        String email,

        @Schema(description = "User password", example = "User123*")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

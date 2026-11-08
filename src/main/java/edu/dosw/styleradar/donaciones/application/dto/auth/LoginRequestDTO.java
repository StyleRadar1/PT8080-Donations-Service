package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para inicio de sesión y obtención de JWT")
public record LoginRequestDTO(
        @Schema(description = "Correo electrónico del usuario", example = "user@styleradar.com")
        @NotBlank(message = "El email es obligatorio")
        String email,

        @Schema(description = "Contraseña de acceso", example = "User123*")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}

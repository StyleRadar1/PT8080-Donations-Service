package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Perfil de usuario de prueba preconfigurado para pruebas en Swagger")
public record DemoUserDTO(
        @Schema(description = "ID del usuario", example = "101")
        Long id,

        @Schema(description = "Correo demo", example = "user@styleradar.com")
        String email,

        @Schema(description = "Contraseña predeterminada", example = "User123*")
        String password,

        @Schema(description = "Rol asignado", example = "USER")
        String role,

        @Schema(description = "Nombre o descripción de la cuenta", example = "Usuario Normal (Donante / Vendedor)")
        String name,

        @Schema(description = "Acciones permitidas en la API", example = "Publicar ropa, editar, vender, retirar, solicitar donaciones, agendar recogida")
        List<String> allowedActions
) {
}

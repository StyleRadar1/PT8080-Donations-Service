package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Respuesta de autenticación exitosa con token Bearer JWT")
public record LoginResponseDTO(
        @Schema(description = "Token Bearer JWT para autorizar peticiones en Swagger y clientes REST")
        String token,

        @Schema(description = "Tipo de esquema de autenticación", example = "Bearer")
        String type,

        @Schema(description = "Segundos de validez del token", example = "86400")
        long expiresIn,

        @Schema(description = "Identificador del usuario", example = "101")
        Long userId,

        @Schema(description = "Correo del usuario", example = "user@styleradar.com")
        String email,

        @Schema(description = "Nombre completo o razón social", example = "Juan Pérez (Usuario Normal)")
        String name,

        @Schema(description = "Rol asignado en el sistema", example = "USER")
        String role,

        @Schema(description = "Lista de autoridades Spring Security", example = "[\"ROLE_USER\"]")
        List<String> authorities
) {
}

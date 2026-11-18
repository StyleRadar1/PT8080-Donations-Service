package edu.dosw.styleradar.donaciones.application.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Preconfigured demo user profile for Swagger testing")
public record DemoUserDTO(
        @Schema(description = "User ID", example = "101")
        Long id,

        @Schema(description = "Demo email", example = "user@styleradar.com")
        String email,

        @Schema(description = "Default demo password", example = "User123*")
        String password,

        @Schema(description = "Assigned role", example = "USER")
        String role,

        @Schema(description = "Account display name", example = "Usuario Normal (Donante / Vendedor)")
        String name,

        @Schema(description = "Allowed actions in the API", example = "Publish garments, edit, sell, retire, request donations, schedule pickup")
        List<String> allowedActions
) {
}

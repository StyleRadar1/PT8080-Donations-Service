package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.dosw.styleradar.donaciones.application.dto.auth.DemoUserDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginResponseDTO;
import edu.dosw.styleradar.donaciones.infrastructure.security.DemoUserService;
import edu.dosw.styleradar.donaciones.infrastructure.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para autenticación, inicio de sesión y obtención de tokens JWT
 * para probar el sistema en Swagger UI con los diferentes roles.
 * Restringido exclusivamente a ambientes locales y de desarrollo controlado (local, test, dev).
 * En QA (qa) y producción (prod) este bean NO se carga, garantizando la seguridad perimetral.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile({"local", "test", "dev"})
@Tag(
        name = "Demo Authentication & Users",
        description = "Endpoints for login and instant JWT token generation (Admin, User, Foundation) for Swagger testing"
)
public class AuthController {

    private final DemoUserService demoUserService;

    @Operation(
            summary = "Log in with credentials",
            description = "Authenticates a demo user by email and password, returning a Bearer JWT token with role and permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful login with JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "SuccessfulLogin",
                                    value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "tokenType": "Bearer",
                                      "expiresIn": 86400,
                                      "userId": 101,
                                      "email": "user@styleradar.com",
                                      "name": "Juan Pérez (Usuario Normal / Donante)",
                                      "role": "USER"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid credentials or malformed request payload",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidCredentials",
                                    value = """
                                    {
                                      "type": "https://api.styleradar.com/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Credenciales inválidas: usuario o contraseña incorrectos",
                                      "instance": "/api/v1/auth/login",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AuthServerError",
                                    value = """
                                    {
                                      "type": "https://api.styleradar.com/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al autenticar credenciales.",
                                      "instance": "/api/v1/auth/login",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = demoUserService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Generate quick JWT token by role",
            description = "Immediately generates a signed Bearer JWT token for any predefined role (ADMIN, USER, FOUNDATION) without requiring manual password entry for Swagger testing."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "JWT token generated for the selected role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "QuickTokenSuccess",
                                    value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "tokenType": "Bearer",
                                      "expiresIn": 86400,
                                      "userId": 101,
                                      "email": "user@styleradar.com",
                                      "name": "Juan Pérez (Usuario Normal / Donante)",
                                      "role": "USER"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid role parameter",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "InvalidRole",
                                    value = """
                                    {
                                      "type": "https://api.styleradar.com/errors/bad-request",
                                      "title": "Solicitud inválida",
                                      "status": 400,
                                      "detail": "Parámetro 'role' no válido. Valores permitidos: ADMIN, USER, FOUNDATION",
                                      "instance": "/api/v1/auth/quick-token",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "QuickTokenServerError",
                                    value = """
                                    {
                                      "type": "https://api.styleradar.com/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al generar el token demo.",
                                      "instance": "/api/v1/auth/quick-token",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/quick-token")
    public ResponseEntity<LoginResponseDTO> quickToken(
            @Parameter(description = "Role to generate token for (ADMIN, USER, or FOUNDATION)", example = "USER")
            @RequestParam Role role) {
        LoginResponseDTO response = demoUserService.quickToken(role);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List preconfigured demo users",
            description = "Returns the catalog of preconfigured demo accounts including their credentials, roles, and allowed actions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of demo users with roles and credentials",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DemoUserDTO.class)),
                            examples = @ExampleObject(
                                    name = "DemoUsersList",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "email": "admin@styleradar.com",
                                        "name": "Administrador StyleRadar",
                                        "role": "ADMIN",
                                        "description": "Acceso total a todas las operaciones y moderación"
                                      },
                                      {
                                        "id": 101,
                                        "email": "user@styleradar.com",
                                        "name": "Juan Pérez (Usuario Normal / Donante)",
                                        "role": "USER",
                                        "description": "Publicación C2C, donaciones y gestión de sus propias prendas"
                                      },
                                      {
                                        "id": 501,
                                        "email": "foundation@styleradar.com",
                                        "name": "Fundación Retorno Verde",
                                        "role": "FOUNDATION",
                                        "description": "Reclamo en Vitrina Abierta, evaluación y recepción física"
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "DemoUsersServerError",
                                    value = """
                                    {
                                      "type": "https://api.styleradar.com/errors/internal-server-error",
                                      "title": "Error interno del servidor",
                                      "status": 500,
                                      "detail": "Error inesperado al obtener los usuarios demo.",
                                      "instance": "/api/v1/auth/demo-users",
                                      "timestamp": "2026-11-11T23:20:00Z"
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping("/demo-users")
    public ResponseEntity<List<DemoUserDTO>> listDemoUsers() {
        return ResponseEntity.ok(demoUserService.listDemoUsers());
    }
}

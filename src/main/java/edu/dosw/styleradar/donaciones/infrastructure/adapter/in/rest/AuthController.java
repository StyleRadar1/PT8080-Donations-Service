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
 * Restringido a ambientes de desarrollo y pruebas controladas (local, test, dev, qa).
 * En producción (prod) este bean NO se carga, garantizando la seguridad perimetral.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Profile({"local", "test", "dev", "qa"})
@Tag(
        name = "Autenticación & Usuarios Demo",
        description = "Endpoints para inicio de sesión, obtención inmediata de tokens JWT (Admin, Usuario Normal, Fundación) para usar en el botón 'Authorize' de Swagger"
)
public class AuthController {

    private final DemoUserService demoUserService;

    @Operation(
            summary = "Iniciar sesión con credenciales",
            description = "Autentica un usuario por email y contraseña, retornando un token Bearer JWT con su rol y permisos correspondientes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesión exitoso con token JWT",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "LoginExitoso",
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
                    description = "Credenciales incorrectas o datos de entrada inválidos",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "CredencialesIncorrectas",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
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
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorAuth",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
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
            summary = "Obtener token JWT rápido por rol",
            description = "Genera instantáneamente un token Bearer JWT firmado para cualquiera de los 3 roles predefinidos (ADMIN, USER, FOUNDATION) sin requerir contraseña manual, para pruebas inmediatas en Swagger."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token JWT generado para el rol seleccionado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "QuickTokenExitoso",
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
                    description = "Rol no especificado o inválido",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "RolInvalido",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/bad-request",
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
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorQuickToken",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
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
            @Parameter(description = "Rol para el cual generar el token (ADMIN, USER o FOUNDATION)", example = "USER")
            @RequestParam Role role) {
        LoginResponseDTO response = demoUserService.quickToken(role);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar usuarios demo preconfigurados",
            description = "Devuelve el catálogo de usuarios de prueba preconfigurados en el sistema con sus credenciales, roles y catálogo de acciones permitidas para cada uno."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios demo con sus roles y credenciales",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DemoUserDTO.class)),
                            examples = @ExampleObject(
                                    name = "ListaUsuariosDemo",
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
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ErrorServidorDemoUsers",
                                    value = """
                                    {
                                      "type": "https://styleradar.dosw.edu/errors/internal-server-error",
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

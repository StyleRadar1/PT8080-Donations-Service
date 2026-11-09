package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.donaciones.application.dto.auth.DemoUserDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginRequestDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginResponseDTO;
import edu.dosw.styleradar.donaciones.infrastructure.security.DemoUserService;
import edu.dosw.styleradar.donaciones.infrastructure.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para autenticación, inicio de sesión y obtención de tokens JWT
 * para probar el sistema en Swagger UI con los diferentes roles.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Profile({"local", "test", "dev"})
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
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso con token JWT",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Credenciales incorrectas o datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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
            @ApiResponse(responseCode = "200", description = "Token JWT generado para el rol seleccionado",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Rol no especificado o inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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
            @ApiResponse(responseCode = "200", description = "Lista de usuarios demo con sus roles y credenciales",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DemoUserDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/demo-users")
    public ResponseEntity<List<DemoUserDTO>> listDemoUsers() {
        return ResponseEntity.ok(demoUserService.listDemoUsers());
    }
}

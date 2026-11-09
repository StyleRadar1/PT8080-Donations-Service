package edu.dosw.styleradar.donaciones.infrastructure.security;

import edu.dosw.styleradar.donaciones.application.dto.auth.DemoUserDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginResponseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que gestiona los usuarios de prueba (Admin, Normal, Fundación)
 * y la emisión de tokens JWT para interactuar con la API en Swagger UI.
 * Restringido a ambientes de desarrollo y pruebas controladas (local, test, dev, qa).
 * En producción (prod) este bean NO se carga, evitando la exposición de identidades o credenciales.
 */
@Service
@Profile({"local", "test", "dev", "qa"})
@SuppressWarnings("java:S2068")
public class DemoUserService {

    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, UserPrincipal> usersByEmail = new ConcurrentHashMap<>();
    private final Map<Role, UserPrincipal> usersByRole = new ConcurrentHashMap<>();

    public DemoUserService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        initializeDemoUsers();
    }

    private void initializeDemoUsers() {
        UserPrincipal admin = new UserPrincipal(
                1L,
                "admin@styleradar.com",
                "Admin123*",
                "Administrador StyleRadar",
                Role.ADMIN
        );

        UserPrincipal normalUser = new UserPrincipal(
                101L,
                "user@styleradar.com",
                "User123*",
                "Juan Pérez (Usuario Normal / Donante)",
                Role.USER
        );

        UserPrincipal foundation = new UserPrincipal(
                501L,
                "foundation@styleradar.com",
                "Foundation123*",
                "Fundación Retorno Verde",
                Role.FOUNDATION
        );

        registerUser(admin);
        registerUser(normalUser);
        registerUser(foundation);
    }

    private void registerUser(UserPrincipal user) {
        usersByEmail.put(user.email().toLowerCase(), user);
        usersByRole.put(user.role(), user);
    }

    public LoginResponseDTO login(String email, String password) {
        if (email == null || password == null) {
            throw new BadCredentialsException("Credenciales inválidas: email y contraseña requeridos");
        }

        UserPrincipal user = usersByEmail.get(email.trim().toLowerCase());
        if (user == null || !user.password().equals(password)) {
            throw new BadCredentialsException("Credenciales inválidas: usuario o contraseña incorrectos");
        }

        return createLoginResponse(user);
    }

    public LoginResponseDTO quickToken(Role role) {
        UserPrincipal user = Optional.ofNullable(usersByRole.get(role))
                .orElseThrow(() -> new IllegalArgumentException("Rol no soportado: " + role));
        return createLoginResponse(user);
    }

    public List<DemoUserDTO> listDemoUsers() {
        return List.of(
                new DemoUserDTO(
                        1L,
                        "admin@styleradar.com",
                        "Admin123*",
                        Role.ADMIN.name(),
                        "Administrador StyleRadar",
                        List.of(
                                "Moderar catálogo completo",
                                "Retirar cualquier publicación",
                                "Ver todas las publicaciones y donaciones",
                                "Aprobar o rechazar solicitudes de donación",
                                "Confirmar recepciones físicas en fundaciones"
                        )
                ),
                new DemoUserDTO(
                        101L,
                        "user@styleradar.com",
                        "User123*",
                        Role.USER.name(),
                        "Juan Pérez (Usuario Normal / Donante)",
                        List.of(
                                "Publicar prendas de segunda mano para reventa C2C",
                                "Editar publicaciones propias",
                                "Marcar publicaciones propias como vendidas o retirarlas",
                                "Destinar prendas propias a donación",
                                "Crear solicitudes de donación a fundaciones",
                                "Programar punto de entrega y franja horaria de recogida",
                                "Consultar historial de donaciones propias"
                        )
                ),
                new DemoUserDTO(
                        501L,
                        "foundation@styleradar.com",
                        "Foundation123*",
                        Role.FOUNDATION.name(),
                        "Fundación Retorno Verde",
                        List.of(
                                "Consultar solicitudes de donación dirigidas a la fundación",
                                "Decidir sobre solicitudes benéficas (Aprobar o Rechazar)",
                                "Confirmar recepción física de las prendas donadas",
                                "Consultar catálogo de prendas disponibles"
                        )
                )
        );
    }

    public UserPrincipal findByEmail(String email) {
        if (email == null) return null;
        return usersByEmail.get(email.trim().toLowerCase());
    }

    public UserPrincipal findById(Long id) {
        return usersByEmail.values().stream()
                .filter(u -> u.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private LoginResponseDTO createLoginResponse(UserPrincipal user) {
        String token = jwtTokenProvider.generateToken(
                user.id(),
                user.email(),
                user.role(),
                user.name()
        );

        return new LoginResponseDTO(
                token,
                "Bearer",
                jwtTokenProvider.getExpiresInSeconds(),
                user.id(),
                user.email(),
                user.name(),
                user.role().name(),
                List.of(user.role().getAuthority())
        );
    }
}

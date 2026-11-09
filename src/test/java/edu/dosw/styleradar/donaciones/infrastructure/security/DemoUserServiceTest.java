package edu.dosw.styleradar.donaciones.infrastructure.security;

import edu.dosw.styleradar.donaciones.application.dto.auth.DemoUserDTO;
import edu.dosw.styleradar.donaciones.application.dto.auth.LoginResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoUserServiceTest {

    private DemoUserService demoUserService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(null, Duration.ofHours(2), null);
        demoUserService = new DemoUserService(jwtTokenProvider);
    }

    @Test
    @DisplayName("Debe autenticar correctamente al Administrador con sus credenciales demo")
    void shouldLoginAdminSuccessfully() {
        LoginResponseDTO response = demoUserService.login("admin@styleradar.com", "Admin123*");

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("admin@styleradar.com");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.token()).isNotBlank();
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.authorities()).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Debe autenticar correctamente al Usuario Normal con sus credenciales demo")
    void shouldLoginNormalUserSuccessfully() {
        LoginResponseDTO response = demoUserService.login("user@styleradar.com", "User123*");

        assertThat(response.userId()).isEqualTo(101L);
        assertThat(response.email()).isEqualTo("user@styleradar.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.token()).isNotBlank();
        assertThat(response.authorities()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Debe autenticar correctamente a la Fundación con sus credenciales demo")
    void shouldLoginFoundationSuccessfully() {
        LoginResponseDTO response = demoUserService.login("foundation@styleradar.com", "Foundation123*");

        assertThat(response.userId()).isEqualTo(501L);
        assertThat(response.email()).isEqualTo("foundation@styleradar.com");
        assertThat(response.role()).isEqualTo("FOUNDATION");
        assertThat(response.token()).isNotBlank();
        assertThat(response.authorities()).contains("ROLE_FOUNDATION");
    }

    @Test
    @DisplayName("Debe lanzar BadCredentialsException si la contraseña o email son incorrectos o nulos")
    void shouldFailOnInvalidCredentials() {
        assertThatThrownBy(() -> demoUserService.login("user@styleradar.com", "clave-erronea"))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> demoUserService.login("inexistente@styleradar.com", "Admin123*"))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> demoUserService.login(null, "Admin123*"))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> demoUserService.login("admin@styleradar.com", null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Debe generar token rápido sin contraseña para cada rol")
    void shouldGenerateQuickTokenForRoles() {
        LoginResponseDTO adminToken = demoUserService.quickToken(Role.ADMIN);
        assertThat(adminToken.role()).isEqualTo("ADMIN");
        assertThat(jwtTokenProvider.validateToken(adminToken.token())).isTrue();

        LoginResponseDTO userToken = demoUserService.quickToken(Role.USER);
        assertThat(userToken.role()).isEqualTo("USER");
        assertThat(jwtTokenProvider.validateToken(userToken.token())).isTrue();

        LoginResponseDTO foundationToken = demoUserService.quickToken(Role.FOUNDATION);
        assertThat(foundationToken.role()).isEqualTo("FOUNDATION");
        assertThat(jwtTokenProvider.validateToken(foundationToken.token())).isTrue();
    }

    @Test
    @DisplayName("Debe listar los 3 usuarios demo preconfigurados con sus acciones permitidas")
    void shouldListDemoUsers() {
        List<DemoUserDTO> demoUsers = demoUserService.listDemoUsers();

        assertThat(demoUsers).hasSize(3);
        assertThat(demoUsers).extracting(DemoUserDTO::role)
                .containsExactlyInAnyOrder("ADMIN", "USER", "FOUNDATION");

        DemoUserDTO userDto = demoUsers.stream()
                .filter(u -> u.role().equals("USER"))
                .findFirst()
                .orElseThrow();
        assertThat(userDto.allowedActions()).isNotEmpty();
    }

    @Test
    @DisplayName("Debe buscar usuario por email y por ID")
    void shouldFindUserByEmailAndId() {
        UserPrincipal userByEmail = demoUserService.findByEmail("USER@styleradar.com");
        assertThat(userByEmail).isNotNull();
        assertThat(userByEmail.id()).isEqualTo(101L);

        UserPrincipal userById = demoUserService.findById(501L);
        assertThat(userById).isNotNull();
        assertThat(userById.email()).isEqualTo("foundation@styleradar.com");

        assertThat(demoUserService.findByEmail(null)).isNull();
        assertThat(demoUserService.findById(9999L)).isNull();
    }
}

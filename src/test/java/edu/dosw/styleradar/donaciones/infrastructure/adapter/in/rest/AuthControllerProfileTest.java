package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.donaciones.infrastructure.security.DemoUserService;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pruebas unitarias de aislamiento por perfil para AuthController y DemoUserService.
 * Garantiza que en el perfil "prod" estos componentes no se instancien, impidiendo
 * la exposición de usuarios demo y generación no autenticada de tokens en producción.
 */
class AuthControllerProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AuthController.class, DemoUserService.class)
            .withBean(JwtTokenProvider.class, () -> mock(JwtTokenProvider.class));

    @Test
    @DisplayName("En perfil prod - AuthController y DemoUserService NO deben registrarse como beans")
    void shouldNotRegisterAuthBeansInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthController.class);
                    assertThat(context).doesNotHaveBean(DemoUserService.class);
                });
    }

    @Test
    @DisplayName("En perfil qa - AuthController y DemoUserService SÍ deben registrarse para pruebas controladas")
    void shouldRegisterAuthBeansInQaProfile() {
        runner.withPropertyValues("spring.profiles.active=qa")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthController.class);
                    assertThat(context).hasSingleBean(DemoUserService.class);
                });
    }

    @Test
    @DisplayName("En perfil dev/local/test - AuthController y DemoUserService SÍ deben registrarse")
    void shouldRegisterAuthBeansInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthController.class);
                    assertThat(context).hasSingleBean(DemoUserService.class);
                });
    }
}

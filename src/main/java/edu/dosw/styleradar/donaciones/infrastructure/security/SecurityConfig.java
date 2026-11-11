package edu.dosw.styleradar.donaciones.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security y control de acceso basado en roles (RBAC) para la API REST.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_FOUNDATION = "FOUNDATION";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityErrorHandler securityErrorHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Documentación Swagger y OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // Actuator y Health Checks
                        .requestMatchers("/actuator/**", "/health").permitAll()

                        // Endpoints de autenticación y tokens demo
                        .requestMatchers("/auth/**").permitAll()

                        // Carga de fotografías a Azure Blob Storage
                        .requestMatchers(HttpMethod.POST, "/segunda-mano/fotos/**", "/donaciones/fotos/**", "/fotos/**").hasAnyRole(ROLE_USER, ROLE_FOUNDATION, ROLE_ADMIN)

                        // Catálogo público de segunda mano (lectura)
                        .requestMatchers(HttpMethod.GET, "/segunda-mano", "/segunda-mano/{id}").permitAll()

                        // Operaciones de Usuario Normal y Administrador sobre publicaciones
                        .requestMatchers(HttpMethod.POST, "/segunda-mano").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/segunda-mano/{id}").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/segunda-mano/{id}/sell").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/segunda-mano/{id}/retire").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/segunda-mano/{id}/donate").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/segunda-mano/user/{userId}").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                        // Operaciones de Donación - Donante (USER) y ADMIN
                        .requestMatchers(HttpMethod.POST, "/donaciones").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/donaciones/{id}/pickup").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/donaciones/donor/{donorId}").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                        // Operaciones de Donación - Fundación (FOUNDATION) y ADMIN
                        .requestMatchers(HttpMethod.PATCH, "/donaciones/{id}/decide").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/donaciones/{id}/confirm").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/donaciones/foundation/{foundationId}").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)

                        // Consulta de solicitud específica (compartida entre los 3 roles autenticados)
                        .requestMatchers(HttpMethod.GET, "/donaciones/{id}").hasAnyRole(ROLE_USER, ROLE_FOUNDATION, ROLE_ADMIN)

                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

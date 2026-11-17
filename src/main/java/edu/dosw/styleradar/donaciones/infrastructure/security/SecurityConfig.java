package edu.dosw.styleradar.donaciones.infrastructure.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    private static final String ROLE_STORE = "STORE";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;

    @Value("${styleradar.cors.allowed-origins:*}")
    private String allowedOrigins = "*";

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
                .cors(Customizer.withDefaults())
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
                                "/api/v1/swagger-ui.html",
                                "/api/v1/swagger-ui/**",
                                "/api/v1/v3/api-docs/**",
                                "/api/v1/v3/api-docs",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // Actuator y Health Checks
                        .requestMatchers("/actuator/**", "/api/v1/health").permitAll()

                        // Endpoints de autenticación y tokens demo
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Carga de fotografías a Azure Blob Storage
                        .requestMatchers(HttpMethod.POST, "/api/v1/second-hand/photos/**", "/api/v1/donations/photos/**")
                        .hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN)

                        // Catálogo público de segunda mano (lectura)
                        .requestMatchers(HttpMethod.GET, "/api/v1/second-hand/publications", "/api/v1/second-hand/publications/{id}").permitAll()

                        // Operaciones sobre publicaciones (Crear, Editar, Vender, Retirar, Donar, Listar por usuario)
                        .requestMatchers(HttpMethod.POST, "/api/v1/second-hand/publications").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/second-hand/publications/{id}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/second-hand/publications/{id}/sale").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/second-hand/publications/{id}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/second-hand/publications/{id}/donation").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/second-hand/publications/users/{userId}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)

                        // Catálogo de fundaciones aliadas activas (selector SR-RF-44 integrado con ms-tiendas)
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations/me")
                        .hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations", "/api/v1/donations/foundations/{foundationId}")
                        .hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN)

                        // Operaciones de Donación - Donante (USER, STORE) y coordinación logística con Fundación (FOUNDATION)
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/pickup").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/donors/{donorId}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN)

                        // Operaciones de Donación - Reclamo en Vitrina y Gestión Fundación (FOUNDATION, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations/claims").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/decision").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/confirmation").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations/{foundationId}/requests").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN)

                        // Consulta de solicitud específica (compartida entre todos los roles autenticados)
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/{id}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN)

                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins.isEmpty() ? List.of("*") : origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

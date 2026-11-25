package edu.dosw.styleradar.donaciones.infrastructure.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final Environment environment;

    @Value("${styleradar.cors.allowed-origins:*}")
    private String allowedOrigins = "*";

    @Value("${styleradar.security.publication-by-id-path}")
    private String publicationByIdPath;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityErrorHandler securityErrorHandler,
                          Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
        this.environment = environment;
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
                .authorizeHttpRequests(auth -> {
                    // Documentación Swagger y OpenAPI
                    auth.requestMatchers(
                            "/api/v1/swagger-ui.html",
                            "/api/v1/swagger-ui/**",
                            "/api/v1/v3/api-docs/**",
                            "/api/v1/v3/api-docs",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/v3/api-docs"
                    ).permitAll();

                    // Actuator y Health Checks
                    auth.requestMatchers("/actuator/**", "/api/v1/health").permitAll();

                    // Endpoints de autenticación y tokens demo (habilitados solo en local, test y dev)
                    if (environment.acceptsProfiles(Profiles.of("local", "test", "dev"))) {
                        auth.requestMatchers("/api/v1/auth/**").permitAll();
                    }

                    // Carga de fotografías a Azure Blob Storage
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/second-hand/photos/**", "/api/v1/donations/photos/**")
                            .hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN);

                    // Catálogo público de segunda mano (lectura)
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/second-hand/publications", publicationByIdPath).permitAll();

                    // Operaciones sobre publicaciones (Crear, Editar, Vender, Retirar, Donar, Listar por usuario)
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/second-hand/publications").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PUT, publicationByIdPath).hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/second-hand/publications/{id}/sale").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.DELETE, publicationByIdPath).hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/second-hand/publications/{id}/donation").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/second-hand/publications/users/{userId}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);

                    // Catálogo de fundaciones aliadas activas (selector SR-RF-44 integrado con ms-tiendas)
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations/representative")
                            .hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations", "/api/v1/donations/foundations/{foundationId}")
                            .hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN);

                    // Operaciones de Donación - Donante (USER, STORE) y coordinación logística con Fundación (FOUNDATION)
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/donations").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/pickup").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations/donors/{donorId}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_ADMIN);

                    // Operaciones de Donación - Reclamo en Vitrina y Gestión Fundación (FOUNDATION, ADMIN)
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/donations/claims").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/decision").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.PATCH, "/api/v1/donations/{id}/confirmation").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN);
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations/foundations/{foundationId}/requests").hasAnyRole(ROLE_FOUNDATION, ROLE_ADMIN);

                    // Consulta de solicitud específica (compartida entre todos los roles autenticados)
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/donations/{id}").hasAnyRole(ROLE_USER, ROLE_STORE, ROLE_FOUNDATION, ROLE_ADMIN);

                    // Cualquier otra petición debe estar autenticada
                    auth.anyRequest().authenticated();
                })
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

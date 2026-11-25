package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Propiedades de configuración para la integración HTTP con ms-tiendas.
 */
@Component
@ConfigurationProperties(prefix = "styleradar.tiendas")
@Getter
@Setter
public class TiendasProperties {

    private String baseUrl = "http://localhost:8140";
    private String foundationsPath;
    private String foundationByIdPath;
    private String foundationByRepresentativePath;
    private boolean validationEnabled = true;
    private String internalUsername = "ms-donaciones";
    private String internalPassword = "";
    private String serviceBearerToken = "";
    private boolean forwardBearerToken = false;
    private String serviceRolesHeaderName = "X-User-Roles";
    private String serviceRolesHeaderValue = "ROLE_ADMIN_STYLERADAR";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
}

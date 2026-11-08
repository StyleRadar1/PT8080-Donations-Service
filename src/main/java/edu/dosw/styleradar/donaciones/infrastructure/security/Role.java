package edu.dosw.styleradar.donaciones.infrastructure.security;

/**
 * Roles del sistema StyleRadar para el control de acceso basado en roles (RBAC).
 */
public enum Role {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
    FOUNDATION("ROLE_FOUNDATION");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}

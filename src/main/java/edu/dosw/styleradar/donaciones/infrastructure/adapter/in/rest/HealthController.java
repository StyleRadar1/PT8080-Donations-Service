package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Monitoreo del estado operativo del microservicio")
public class HealthController {

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version = "0.0.1-SNAPSHOT";

    @GetMapping
    @Operation(summary = "Diagnóstico y salud del servicio", description = "Verifica que el microservicio esté en ejecución y respondiendo peticiones.")
    @ApiResponse(responseCode = "200", description = "Servicio activo y saludable")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-segunda-vida-donaciones",
                "version", version,
                "port", 8520,
                "timestamp", LocalDateTime.now(ZoneOffset.UTC).toString()
        ));
    }
}

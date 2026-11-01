package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Endpoints de verificación y diagnóstico del servicio")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verificar estado operativo", description = "Comprueba que el microservicio esté en ejecución y respondiendo en el puerto asignado.")
    @ApiResponse(responseCode = "200", description = "Servicio UP y saludable")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-segunda-vida-donaciones",
                "port", 8093,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

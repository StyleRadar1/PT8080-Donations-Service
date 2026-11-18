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
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Service operational status and health monitoring")
public class HealthController {

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version = "0.0.1-SNAPSHOT";

    @Value("${app.commit-sha:${APP_COMMIT_SHA:local-dev}}")
    private String commitSha = "local-dev";

    public HealthController() {
    }

    public HealthController(String version, String commitSha) {
        this.version = version;
        this.commitSha = commitSha;
    }

    @GetMapping
    @Operation(summary = "Service health and diagnostics", description = "Verifies that the microservice is running and responding to requests.")
    @ApiResponse(responseCode = "200", description = "Service is up and healthy")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-segunda-vida-donaciones",
                "version", version,
                "commitSha", commitSha,
                "port", 8520,
                "timestamp", LocalDateTime.now(ZoneOffset.UTC).toString()
        ));
    }
}

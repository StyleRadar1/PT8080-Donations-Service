package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import edu.dosw.styleradar.donaciones.domain.model.ConservationStatus;
import edu.dosw.styleradar.donaciones.domain.model.GarmentType;
import edu.dosw.styleradar.donaciones.domain.model.LocalidadBogota;
import edu.dosw.styleradar.donaciones.domain.model.Size;

/**
 * Configuración WebMVC para conversión tolerante de parámetros query (@RequestParam)
 * hacia los enums de categoría de prenda, talla, estado de conservación y localidad de Bogotá.
 */
@Configuration
public class WebMvcEnumConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, GarmentType.class, GarmentType::fromString);
        registry.addConverter(String.class, Size.class, Size::fromString);
        registry.addConverter(String.class, ConservationStatus.class, ConservationStatus::fromString);
        registry.addConverter(String.class, LocalidadBogota.class, LocalidadBogota::fromString);
    }
}

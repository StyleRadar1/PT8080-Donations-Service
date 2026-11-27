package edu.dosw.styleradar.donaciones.infrastructure.config;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.filter.CorrelationIdFilter;

/**
 * Configuración del cliente HTTP RestClient para la comunicación con ms-tiendas.
 * Propaga automáticamente las cabeceras de trazabilidad {@code X-Correlation-Id} y {@code X-Request-Id}
 * desde el contexto MDC hacia las peticiones salientes.
 */
@Configuration
public class TiendasClientConfig {

    @Bean
    public RestClient tiendasRestClient(RestClient.Builder builder, TiendasProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(correlationIdPropagationInterceptor())
                .build();
    }

    public static ClientHttpRequestInterceptor correlationIdPropagationInterceptor() {
        return (request, body, execution) -> {
            copyMdcHeaderIfPresent(
                    request.getHeaders(),
                    CorrelationIdFilter.CORRELATION_ID_MDC_KEY,
                    CorrelationIdFilter.CORRELATION_ID_HEADER);
            copyMdcHeaderIfPresent(
                    request.getHeaders(),
                    CorrelationIdFilter.REQUEST_ID_MDC_KEY,
                    CorrelationIdFilter.REQUEST_ID_HEADER);
            return execution.execute(request, body);
        };
    }

    private static void copyMdcHeaderIfPresent(
            org.springframework.http.HttpHeaders headers, String mdcKey, String headerName) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank() && !headers.containsKey(headerName)) {
            headers.set(headerName, value.trim());
        }
    }
}

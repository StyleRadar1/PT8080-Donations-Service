package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest.filter;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("BUG-031: Preserva X-Correlation-Id y X-Request-Id en MDC y respuesta")
    void shouldPreserveCorrelationAndRequestIds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/donations");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-don-101");
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "req-don-202");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorr = new AtomicReference<>();
        AtomicReference<String> mdcReq = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcCorr.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            mdcReq.set(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(mdcCorr.get()).isEqualTo("corr-don-101");
        assertThat(mdcReq.get()).isEqualTo("req-don-202");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-don-101");
        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-don-202");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("BUG-031: Genera IDs de correlación cuando faltan o usa X-Request-Id como respaldo")
    void shouldGenerateCorrelationIdsWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/donations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNotBlank();
            assertThat(MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY))
                    .isEqualTo(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        });

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER))
                .isEqualTo(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));

        MockHttpServletRequest fallbackReq = new MockHttpServletRequest("GET", "/api/v1/donations");
        fallbackReq.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "req-only-55");
        MockHttpServletResponse fallbackRes = new MockHttpServletResponse();
        filter.doFilter(fallbackReq, fallbackRes, (req, res) -> {
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isEqualTo("req-only-55");
        });
        assertThat(fallbackRes.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("req-only-55");
    }
}

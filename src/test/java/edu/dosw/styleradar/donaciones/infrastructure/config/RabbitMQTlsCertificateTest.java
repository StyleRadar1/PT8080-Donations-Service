package edu.dosw.styleradar.donaciones.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQTlsCertificateTest {

    private static final String EXPECTED_SHA256_FINGERPRINT =
            "76:D7:98:83:70:39:A5:69:71:A6:22:63:DF:6C:97:B4:C7:12:A5:C6:88:37:9C:A1:5F:32:0C:28:D6:0D:DD:D7";

    @Test
    @DisplayName("Debe cargar rabbitmq-truststore.jks en formato PKCS12 y verificar huella SHA-256 de la Root CA")
    void shouldLoadPkcs12TruststoreAndVerifyFingerprint() throws Exception {
        ClassPathResource resource = new ClassPathResource("certs/rabbitmq-truststore.jks");
        assertThat(resource.exists()).isTrue();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, "changeit".toCharArray());
        }

        Enumeration<String> aliases = keyStore.aliases();
        assertThat(aliases.hasMoreElements()).isTrue();

        String alias = aliases.nextElement();
        Certificate cert = keyStore.getCertificate(alias);
        assertThat(cert).isNotNull();

        String fingerprint = computeSha256Fingerprint(cert);
        assertThat(fingerprint).isEqualTo(EXPECTED_SHA256_FINGERPRINT);
    }

    @Test
    @DisplayName("Debe cargar rabbitmq-ca.pem y verificar que coincide con la huella SHA-256 oficial de StyleRadar")
    void shouldLoadPemCertificateAndVerifyFingerprint() throws Exception {
        ClassPathResource resource = new ClassPathResource("certs/rabbitmq-ca.pem");
        assertThat(resource.exists()).isTrue();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        try (InputStream is = resource.getInputStream()) {
            cert = (X509Certificate) cf.generateCertificate(is);
        }

        assertThat(cert).isNotNull();
        assertThat(cert.getSubjectX500Principal().getName()).contains("StyleRadar-Root-CA");

        String fingerprint = computeSha256Fingerprint(cert);
        assertThat(fingerprint).isEqualTo(EXPECTED_SHA256_FINGERPRINT);
    }

    private static String computeSha256Fingerprint(Certificate cert) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        return HexFormat.ofDelimiter(":").withUpperCase().formatHex(digest);
    }
}

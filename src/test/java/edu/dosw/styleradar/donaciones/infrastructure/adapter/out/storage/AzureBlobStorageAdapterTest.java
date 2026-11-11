package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AzureBlobStorageAdapterTest {

    @Test
    @DisplayName("Debe operar en modo fallback local/pruebas cuando no hay connection-string configurada")
    void shouldStorePhotoInLocalFallbackWhenNoConnectionString() {
        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net"
        );

        byte[] content = "fake-image-bytes".getBytes();
        String filename = "garment_test123.jpg";
        String contentType = "image/jpeg";

        String url = adapter.storePhoto(content, filename, contentType);

        assertThat(url).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_test123.jpg");
        assertThat(adapter.getContainerName()).isEqualTo("donaciones-fotos");
        assertThat(adapter.getBlobEndpoint()).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net");
    }

    @Test
    @DisplayName("Debe normalizar slashes en blobEndpoint")
    void shouldNormalizeTrailingSlashesInBlobEndpoint() {
        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net/"
        );

        byte[] content = "test".getBytes();
        String filename = "foto.png";

        String url = adapter.storePhoto(content, filename, "image/png");

        assertThat(url).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/foto.png");
    }
}

package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureBlobStorageAdapterTest {

    @Test
    @DisplayName("Debe operar en modo fallback local/pruebas cuando no hay connection-string en perfil test")
    void shouldStorePhotoInLocalFallbackWhenNoConnectionStringInTestProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net",
                env
        );

        byte[] content = "fake-image-bytes".getBytes();
        String filename = "garment_test123.jpg";
        String contentType = "image/jpeg";

        String url = adapter.storePhoto(content, filename, contentType);

        assertThat(url).isEqualTo("/uploads/garments/garment_test123.jpg");
        assertThat(adapter.getContainerName()).isEqualTo("donaciones-fotos");
        assertThat(adapter.getBlobEndpoint()).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net");
    }

    @Test
    @DisplayName("En perfil qa o prod debe lanzar excepción si no hay connection-string configurada")
    void shouldThrowExceptionInQaOrProdWhenNoConnectionString() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("qa");

        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net",
                env
        );

        byte[] content = "test".getBytes();

        assertThatThrownBy(() -> adapter.storePhoto(content, "foto.png", "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_STORAGE_CONNECTION_STRING es obligatoria");
    }

    @Test
    @DisplayName("Cuando no hay perfiles activos configurados, no debe permitir fallback y debe lanzar excepción")
    void shouldThrowExceptionWhenNoActiveProfilesConfigured() {
        MockEnvironment env = new MockEnvironment(); // Sin perfiles activos

        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net",
                env
        );

        byte[] content = "test".getBytes();

        assertThatThrownBy(() -> adapter.storePhoto(content, "foto.png", "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_STORAGE_CONNECTION_STRING es obligatoria. El fallback local solo está permitido en perfiles local, dev y test.");
    }

    @Test
    @DisplayName("Cuando environment es nulo, no debe permitir fallback y debe lanzar excepción")
    void shouldThrowExceptionWhenEnvIsNull() {
        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net",
                null
        );

        byte[] content = "test".getBytes();

        assertThatThrownBy(() -> adapter.storePhoto(content, "foto.png", "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_STORAGE_CONNECTION_STRING es obligatoria. El fallback local solo está permitido en perfiles local, dev y test.");
    }

    @Test
    @DisplayName("En perfil prod debe lanzar excepción si falla la conexión a Azure Blob")
    void shouldThrowExceptionInProdWhenAzureUploadFails() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                "DefaultEndpointsProtocol=https;AccountName=fake;AccountKey=fakekey;EndpointSuffix=core.windows.net",
                "donaciones-fotos",
                "https://srdonacionesjmunar.blob.core.windows.net",
                env
        );

        byte[] content = "test".getBytes();

        assertThatThrownBy(() -> adapter.storePhoto(content, "foto.png", "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Error al almacenar fotografía en Azure Blob Storage");
    }
}

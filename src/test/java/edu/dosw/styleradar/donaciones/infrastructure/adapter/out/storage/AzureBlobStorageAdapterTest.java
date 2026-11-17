package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.mock.env.MockEnvironment;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("Debe subir fotografía a Azure Blob Storage creando contenedor si no existe y asignando Content-Type")
    void shouldUploadPhotoToAzureCreatingContainerAndSettingHeaders() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        BlobServiceClient mockServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
        BlobClient mockBlobClient = mock(BlobClient.class);

        when(mockServiceClient.getBlobContainerClient("custom-container")).thenReturn(mockContainerClient);
        when(mockContainerClient.exists()).thenReturn(false);
        when(mockContainerClient.getBlobClient("jacket.jpg")).thenReturn(mockBlobClient);
        when(mockBlobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/custom-container/jacket.jpg");

        try (MockedConstruction<BlobServiceClientBuilder> ignored = mockConstruction(BlobServiceClientBuilder.class,
                (mockBuilder, context) -> {
                    when(mockBuilder.connectionString(anyString())).thenReturn(mockBuilder);
                    when(mockBuilder.buildClient()).thenReturn(mockServiceClient);
                })) {
            AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                    "DefaultEndpointsProtocol=https;AccountName=real;AccountKey=key;EndpointSuffix=core.windows.net",
                    "  custom-container  ",
                    "  https://custom.blob.core.windows.net  ",
                    env
            );

            String url = adapter.storePhoto("img-bytes".getBytes(), "jacket.jpg", "image/jpeg");

            assertThat(url).isEqualTo("https://account.blob.core.windows.net/custom-container/jacket.jpg");
            assertThat(adapter.getContainerName()).isEqualTo("custom-container");
            assertThat(adapter.getBlobEndpoint()).isEqualTo("https://custom.blob.core.windows.net");
            verify(mockContainerClient).create();
            verify(mockBlobClient).upload(any(InputStream.class), anyLong(), anyBoolean());
            verify(mockBlobClient).setHttpHeaders(any(BlobHttpHeaders.class));
        }
    }

    @Test
    @DisplayName("Debe subir fotografía a contenedor Azure existente sin setHttpHeaders cuando contentType es nulo o vacío")
    void shouldUploadPhotoToExistingAzureContainerWithoutContentType() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("qa");

        BlobServiceClient mockServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
        BlobClient mockBlobClient = mock(BlobClient.class);

        when(mockServiceClient.getBlobContainerClient("donaciones-fotos")).thenReturn(mockContainerClient);
        when(mockContainerClient.exists()).thenReturn(true);
        when(mockContainerClient.getBlobClient("photo.webp")).thenReturn(mockBlobClient);
        when(mockBlobClient.getBlobUrl()).thenReturn("https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/photo.webp");

        try (MockedConstruction<BlobServiceClientBuilder> ignored = mockConstruction(BlobServiceClientBuilder.class,
                (mockBuilder, context) -> {
                    when(mockBuilder.connectionString(anyString())).thenReturn(mockBuilder);
                    when(mockBuilder.buildClient()).thenReturn(mockServiceClient);
                })) {
            AzureBlobStorageAdapter adapter = new AzureBlobStorageAdapter(
                    "DefaultEndpointsProtocol=https;AccountName=real;AccountKey=key;EndpointSuffix=core.windows.net",
                    " ",
                    null,
                    env
            );

            String urlWithNullContentType = adapter.storePhoto("webp-bytes".getBytes(), "photo.webp", null);
            String urlWithBlankContentType = adapter.storePhoto("webp-bytes".getBytes(), "photo.webp", "   ");

            assertThat(urlWithNullContentType).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/photo.webp");
            assertThat(urlWithBlankContentType).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/photo.webp");
            assertThat(adapter.getContainerName()).isEqualTo("donaciones-fotos");
            assertThat(adapter.getBlobEndpoint()).isEqualTo("https://srdonacionesjmunar.blob.core.windows.net");
            verify(mockContainerClient, never()).create();
            verify(mockBlobClient, never()).setHttpHeaders(any(BlobHttpHeaders.class));
        }
    }

    @Test
    @DisplayName("En perfiles local y dev debe aplicar fallback local si falla Azure y tolerar IOException al guardar copia")
    void shouldFallbackToLocalInLocalAndDevProfilesAndHandleConstructorDefaults() {
        MockEnvironment localEnv = new MockEnvironment();
        localEnv.setActiveProfiles("local");

        AzureBlobStorageAdapter localFallbackAdapter = new AzureBlobStorageAdapter(
                "DefaultEndpointsProtocol=https;AccountName=fake;AccountKey=fakekey;EndpointSuffix=core.windows.net",
                null,
                " ",
                localEnv
        );

        String urlFromLocalFallback = localFallbackAdapter.storePhoto("bytes".getBytes(), "local_fallback.png", "image/png");
        assertThat(urlFromLocalFallback).isEqualTo("/uploads/garments/local_fallback.png");

        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        AzureBlobStorageAdapter devAdapter = new AzureBlobStorageAdapter(null, "donaciones-fotos", "https://endpoint", devEnv);

        String urlFromDevWithIoFallback = devAdapter.storePhoto("bytes".getBytes(), "missing-subdir/ignored.png", "image/png");
        assertThat(urlFromDevWithIoFallback).isEqualTo("/uploads/garments/missing-subdir/ignored.png");

        AzureBlobStorageAdapter threeArgAdapter = new AzureBlobStorageAdapter(null, "donaciones-fotos", "https://endpoint");
        assertThatThrownBy(() -> threeArgAdapter.storePhoto("bytes".getBytes(), "foto.jpg", "image/jpeg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_STORAGE_CONNECTION_STRING es obligatoria");
    }
}

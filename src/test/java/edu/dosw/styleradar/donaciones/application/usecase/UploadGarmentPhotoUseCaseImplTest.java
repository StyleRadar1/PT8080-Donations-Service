package edu.dosw.styleradar.donaciones.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.port.out.PhotoStoragePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadGarmentPhotoUseCaseImplTest {

    @Mock
    private PhotoStoragePort photoStoragePort;

    private UploadGarmentPhotoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UploadGarmentPhotoUseCaseImpl(photoStoragePort);
    }

    @Test
    @DisplayName("Debe cargar exitosamente una imagen válida JPEG y retornar su URL")
    void shouldUploadValidJpegPhotoSuccessfully() {
        byte[] content = "fake-jpeg-bytes".getBytes();
        String originalFilename = "prenda.jpg";
        String contentType = "image/jpeg";
        String expectedUrl = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_abc.jpg";

        when(photoStoragePort.storePhoto(eq(content), any(String.class), eq(contentType)))
                .thenReturn(expectedUrl);

        String resultUrl = useCase.uploadPhoto(content, originalFilename, contentType);

        assertThat(resultUrl).isEqualTo(expectedUrl);
        verify(photoStoragePort).storePhoto(eq(content), any(String.class), eq(contentType));
    }

    @Test
    @DisplayName("Debe cargar exitosamente una imagen PNG y asignar extensión .png")
    void shouldUploadValidPngPhotoSuccessfully() {
        byte[] content = "fake-png-bytes".getBytes();
        String originalFilename = "foto.png";
        String contentType = "image/png";
        String expectedUrl = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_123.png";

        when(photoStoragePort.storePhoto(eq(content), any(String.class), eq(contentType)))
                .thenReturn(expectedUrl);

        String resultUrl = useCase.uploadPhoto(content, originalFilename, contentType);

        assertThat(resultUrl).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Debe fallar cuando el contenido es nulo o vacío")
    void shouldFailWhenContentIsEmpty() {
        assertThatThrownBy(() -> useCase.uploadPhoto(new byte[0], "foto.jpg", "image/jpeg"))
                .isInstanceOf(InvalidPublicationException.class)
                .hasMessageContaining("no puede estar vacío");

        assertThatThrownBy(() -> useCase.uploadPhoto(null, "foto.jpg", "image/jpeg"))
                .isInstanceOf(InvalidPublicationException.class)
                .hasMessageContaining("no puede estar vacío");
    }

    @Test
    @DisplayName("Debe fallar cuando el tamaño excede 5 MB")
    void shouldFailWhenSizeExceedsLimit() {
        byte[] hugeContent = new byte[(int) UploadGarmentPhotoUseCaseImpl.MAX_FILE_SIZE_BYTES + 1];

        assertThatThrownBy(() -> useCase.uploadPhoto(hugeContent, "pesada.jpg", "image/jpeg"))
                .isInstanceOf(InvalidPublicationException.class)
                .hasMessageContaining("no puede superar los 5 MB");
    }

    @Test
    @DisplayName("Debe fallar cuando el tipo de contenido no es permitido (ej. text/plain o pdf)")
    void shouldFailWhenContentTypeIsNotAllowed() {
        byte[] content = "not an image".getBytes();

        assertThatThrownBy(() -> useCase.uploadPhoto(content, "document.pdf", "application/pdf"))
                .isInstanceOf(InvalidPublicationException.class)
                .hasMessageContaining("Tipo de archivo no permitido");

        assertThatThrownBy(() -> useCase.uploadPhoto(content, "script.sh", "text/plain"))
                .isInstanceOf(InvalidPublicationException.class)
                .hasMessageContaining("Tipo de archivo no permitido");
    }
}

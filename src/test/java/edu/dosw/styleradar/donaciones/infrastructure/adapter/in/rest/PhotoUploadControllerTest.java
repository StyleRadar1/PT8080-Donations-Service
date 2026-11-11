package edu.dosw.styleradar.donaciones.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.port.in.UploadGarmentPhotoUseCase;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtAuthenticationFilter;
import edu.dosw.styleradar.donaciones.infrastructure.security.JwtTokenProvider;
import edu.dosw.styleradar.donaciones.infrastructure.security.ResourceOwnershipValidator;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityConfig;
import edu.dosw.styleradar.donaciones.infrastructure.security.SecurityErrorHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoUploadController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        SecurityErrorHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class PhotoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadGarmentPhotoUseCase uploadGarmentPhotoUseCase;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/segunda-mano/fotos/upload - Debe subir foto exitosamente y responder 201 Created")
    void shouldUploadPhotoSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "chaqueta.jpg",
                "image/jpeg",
                "test-image-content".getBytes()
        );

        String expectedUrl = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_9b83b9c0.jpg";
        when(uploadGarmentPhotoUseCase.uploadPhoto(any(), eq("chaqueta.jpg"), eq("image/jpeg")))
                .thenReturn(expectedUrl);

        mockMvc.perform(multipart("/segunda-mano/fotos/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value(expectedUrl))
                .andExpect(jsonPath("$.filename").value("garment_9b83b9c0.jpg"))
                .andExpect(jsonPath("$.sizeBytes").value(file.getSize()));
    }

    @Test
    @WithMockUser(roles = "FOUNDATION")
    @DisplayName("POST /api/v1/segunda-mano/fotos/upload - Debe permitir carga para rol FOUNDATION y responder 201")
    void shouldUploadPhotoUnderDonationsAlias() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pantalon.png",
                "image/png",
                "png-bytes".getBytes()
        );

        String expectedUrl = "https://srdonacionesjmunar.blob.core.windows.net/donaciones-fotos/garment_pantalon.png";
        when(uploadGarmentPhotoUseCase.uploadPhoto(any(), eq("pantalon.png"), eq("image/png")))
                .thenReturn(expectedUrl);

        mockMvc.perform(multipart("/segunda-mano/fotos/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value(expectedUrl));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/segunda-mano/fotos/upload - Debe responder 400 Bad Request cuando el archivo está vacío")
    void shouldReturn400WhenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vacio.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/segunda-mano/fotos/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/segunda-mano/fotos/upload - Debe responder 400 cuando el caso de uso rechaza tipo de archivo")
    void shouldReturn400WhenUseCaseRejectsFileType() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "pdf-bytes".getBytes()
        );

        when(uploadGarmentPhotoUseCase.uploadPhoto(any(), eq("documento.pdf"), eq("application/pdf")))
                .thenThrow(new InvalidPublicationException("Tipo de archivo no permitido"));

        mockMvc.perform(multipart("/segunda-mano/fotos/upload").file(pdfFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tipo de archivo no permitido"));
    }

    @Test
    @DisplayName("POST /api/v1/segunda-mano/fotos/upload - Debe responder 401 Unauthorized sin autenticación")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "bytes".getBytes()
        );

        mockMvc.perform(multipart("/segunda-mano/fotos/upload").file(file))
                .andExpect(status().isUnauthorized());
    }
}

package edu.dosw.styleradar.donaciones.application.usecase;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.dosw.styleradar.donaciones.domain.exception.InvalidPublicationException;
import edu.dosw.styleradar.donaciones.domain.port.in.UploadGarmentPhotoUseCase;
import edu.dosw.styleradar.donaciones.domain.port.out.PhotoStoragePort;

/**
 * Caso de uso para la validación y carga de fotografías asociadas a prendas de segunda mano y donaciones.
 */
@Service
public class UploadGarmentPhotoUseCaseImpl implements UploadGarmentPhotoUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadGarmentPhotoUseCaseImpl.class);

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final PhotoStoragePort photoStoragePort;

    public UploadGarmentPhotoUseCaseImpl(PhotoStoragePort photoStoragePort) {
        this.photoStoragePort = photoStoragePort;
    }

    @Override
    public String uploadPhoto(byte[] content, String originalFilename, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidPublicationException("El archivo de imagen no puede estar vacío");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new InvalidPublicationException("El tamaño de la imagen no puede superar los 5 MB (tamaño recibido: " + content.length + " bytes)");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidPublicationException("Tipo de archivo no permitido: " + contentType + ". Solo se admiten formatos JPEG, PNG y WEBP.");
        }

        String extension = extractExtension(originalFilename, contentType);
        String uniqueFilename = "garment_" + UUID.randomUUID().toString().replace("-", "") + extension;

        log.info("Almacenando fotografía '{}' ({} bytes, tipo '{}') a través de PhotoStoragePort",
                uniqueFilename, content.length, contentType);

        return photoStoragePort.storePhoto(content, uniqueFilename, contentType);
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
                return ext;
            }
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}

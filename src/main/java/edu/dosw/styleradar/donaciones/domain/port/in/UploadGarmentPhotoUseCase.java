package edu.dosw.styleradar.donaciones.domain.port.in;

/**
 * Puerto de entrada para la carga y almacenamiento de fotografías de prendas y donaciones.
 */
public interface UploadGarmentPhotoUseCase {

    /**
     * Valida, procesa y almacena una fotografía en el almacenamiento de objetos.
     *
     * @param content          Bytes binarios de la imagen.
     * @param originalFilename Nombre original del archivo proporcionado por el usuario.
     * @param contentType      Tipo MIME reportado por el cliente (ej. image/jpeg, image/png).
     * @return URL pública HTTPS accesible para visualizar la fotografía en la vitrina.
     */
    String uploadPhoto(byte[] content, String originalFilename, String contentType);
}

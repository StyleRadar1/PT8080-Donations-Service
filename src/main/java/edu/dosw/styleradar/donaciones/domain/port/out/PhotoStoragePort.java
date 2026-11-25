package edu.dosw.styleradar.donaciones.domain.port.out;

/**
 * Puerto de salida para persistencia de fotografías en Azure Blob Storage o almacenamiento físico.
 */
public interface PhotoStoragePort {

    /**
     * Persiste los bytes de la fotografía y retorna su URL pública de acceso.
     *
     * @param content     Bytes binarios del archivo.
     * @param filename    Nombre único generado para el archivo en el contenedor.
     * @param contentType Tipo MIME de la imagen (ej. image/jpeg, image/png).
     * @return URL pública absoluta de acceso al archivo almacenado.
     */
    String storePhoto(byte[] content, String filename, String contentType);
}

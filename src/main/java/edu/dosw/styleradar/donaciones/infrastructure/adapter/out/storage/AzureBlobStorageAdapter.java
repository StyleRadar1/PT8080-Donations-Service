package edu.dosw.styleradar.donaciones.infrastructure.adapter.out.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import edu.dosw.styleradar.donaciones.domain.port.out.PhotoStoragePort;

/**
 * Adaptador de infraestructura para persistencia de fotografías en Azure Blob Storage.
 * Implementa el puerto PhotoStoragePort dentro de la arquitectura hexagonal.
 * En QA y Producción exige credenciales reales y reporta errores reales hacia la nube.
 * En local/test permite modo de desarrollo con almacenamiento local.
 */
@Component
public class AzureBlobStorageAdapter implements PhotoStoragePort {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageAdapter.class);

    private final String connectionString;
    private final String containerName;
    private final String blobEndpoint;
    private final Environment env;

    @Autowired
    public AzureBlobStorageAdapter(
            @Value("${azure.storage.connection-string:}") String connectionString,
            @Value("${azure.storage.container-name:donaciones-fotos}") String containerName,
            @Value("${azure.storage.blob-endpoint:https://srdonacionesjmunar.blob.core.windows.net}") String blobEndpoint,
            Environment env) {
        this.connectionString = connectionString;
        this.containerName = (containerName == null || containerName.isBlank()) ? "donaciones-fotos" : containerName.trim();
        this.blobEndpoint = (blobEndpoint == null || blobEndpoint.isBlank()) ? "https://srdonacionesjmunar.blob.core.windows.net" : blobEndpoint.trim();
        this.env = env;
    }

    public AzureBlobStorageAdapter(
            String connectionString,
            String containerName,
            String blobEndpoint) {
        this(connectionString, containerName, blobEndpoint, null);
    }

    @Override
    public String storePhoto(byte[] content, String filename, String contentType) {
        boolean isLocalOrTest = isLocalOrTestProfile();

        if (connectionString != null && !connectionString.isBlank()) {
            try {
                log.info("Subiendo fotografía '{}' a contenedor Azure '{}'...", filename, containerName);
                BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();

                BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
                if (!containerClient.exists()) {
                    containerClient.create();
                    log.info("Contenedor '{}' creado exitosamente en Azure Blob Storage", containerName);
                }

                BlobClient blobClient = containerClient.getBlobClient(filename);
                blobClient.upload(new ByteArrayInputStream(content), content.length, true);

                if (contentType != null && !contentType.isBlank()) {
                    BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
                    blobClient.setHttpHeaders(headers);
                }

                String blobUrl = blobClient.getBlobUrl();
                log.info("Fotografía subida exitosamente a Azure Blob Storage. URL pública: {}", blobUrl);
                return blobUrl;
            } catch (Exception ex) {
                if (!isLocalOrTest) {
                    log.error("Fallo crítico al subir fotografía a Azure Blob Storage en entorno de despliegue: {}", ex.getMessage());
                    throw new IllegalStateException("Error al almacenar fotografía en Azure Blob Storage: " + ex.getMessage(), ex);
                }
                log.warn("Fallo temporal al subir a Azure Blob Storage ({}). Aplicando fallback local de pruebas: {}",
                        ex.getClass().getSimpleName(), ex.getMessage());
            }
        } else {
            if (!isLocalOrTest) {
                log.error("AZURE_STORAGE_CONNECTION_STRING no configurada fuera de perfiles permitidos (local, dev, test).");
                throw new IllegalStateException("La variable de entorno AZURE_STORAGE_CONNECTION_STRING es obligatoria. El fallback local solo está permitido en perfiles local, dev y test.");
            }
            log.info("Sin connection string de Azure configurada. Operando en modo local/pruebas para '{}'", filename);
        }

        // Respaldo local exclusivamente para entornos de desarrollo y pruebas
        saveLocalCopy(content, filename);
        String localUrl = "/uploads/garments/" + filename;
        log.info("Fotografía guardada en almacenamiento local de pruebas: {}", localUrl);
        return localUrl;
    }

    private boolean isLocalOrTestProfile() {
        if (env == null) {
            return false;
        }
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        return activeProfiles.contains("local")
                || activeProfiles.contains("test")
                || activeProfiles.contains("dev");
    }

    private void saveLocalCopy(byte[] content, String filename) {
        try {
            File dir = new File("uploads/garments");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File target = new File(dir, filename);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(content);
            }
        } catch (IOException e) {
            log.debug("No se pudo escribir copia local (no crítico): {}", e.getMessage());
        }
    }

    public String getContainerName() {
        return containerName;
    }

    public String getBlobEndpoint() {
        return blobEndpoint;
    }
}

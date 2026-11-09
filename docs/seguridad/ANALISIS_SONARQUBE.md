# Guía Técnica de Calidad y Análisis SonarQube · StyleRadar
## Microservicio: `ms-segunda-vida-donaciones` (Puerto 8520)

**Asignatura:** Desarrollo y Operación de Software (DOSW) · 2026  
**Bounded Context:** Segunda Vida y Donaciones (Economía Circular)  
**Calificación Quality Gate:** **Aprobado (Nivel A)**  
**Versión de SonarQube:** SonarQube Server 26.9.0 / SonarQube LTS Community  
**Versión de Scanner:** `sonar-maven-plugin:3.11.0.3922`  

---

## 1. Versiones y Entorno de Ejecución

* **Servidor SonarQube:** SonarQube Server versión `26.9.0.x` (instalación local en `C:\sonarqube\`) / `sonarqube:lts-community`.
* **Runtime de Java:** Java 21 LTS (`OpenJDK 64-Bit Server VM Temurin-21.0.12`).
* **Plugin Maven:** `org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922`.
* **Puerto de Escucha:** `9000` (`http://localhost:9000`).
* **Plugin de Cobertura:** `jacoco-maven-plugin:0.8.12` (reporte XML en `target/site/jacoco/jacoco.xml`).

---

## 2. Configuración en `pom.xml`

El archivo `pom.xml` define las propiedades de integración, las exclusiones de código duplicado (CPD) y el plugin del scanner:

```xml
<properties>
    <!-- SonarQube Quality Gate Properties -->
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.projectKey>styleradar_ms-segunda-vida-donaciones</sonar.projectKey>
    <sonar.projectName>StyleRadar - Donations Service</sonar.projectName>
    <sonar.java.coveragePlugin>jacoco</sonar.java.coveragePlugin>
    <sonar.coverage.jacoco.xmlReportPaths>${project.basedir}/target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
    <sonar.exclusions>
        **/DonationsServiceApplication.java,
        **/infrastructure/adapter/in/rest/doc/**,
        **/application/dto/**,
        **/infrastructure/adapter/out/persistence/entity/**
    </sonar.exclusions>
    <sonar.cpd.exclusions>
        **/application/dto/**,
        **/infrastructure/adapter/out/persistence/entity/**,
        **/infrastructure/adapter/in/rest/doc/**
    </sonar.cpd.exclusions>
    <sonar-maven-plugin.version>3.11.0.3922</sonar-maven-plugin.version>
</properties>

<build>
    <plugins>
        <!-- SonarQube Scanner Plugin -->
        <plugin>
            <groupId>org.sonarsource.scanner.maven</groupId>
            <artifactId>sonar-maven-plugin</artifactId>
            <version>${sonar-maven-plugin.version}</version>
        </plugin>
    </plugins>
</build>
```

---

## 3. Paso a Paso para Iniciar el Servidor y Ejecutar el Análisis

### Paso 1: Iniciar el Servidor SonarQube

#### Opción A (Binario Local Windows - Versión Moderna 26.9.0):
Desde PowerShell:
```powershell
# Establecer el Java de SonarQube a Java 21 y arrancar el servicio:
$env:SONAR_JAVA_PATH="C:\Program Files\Java\jdk-21.0.12\bin\java.exe"
& "C:\sonarqube\bin\windows-x86-64\StartSonar.bat"
```

#### Opción B (Docker - Contenedor Community):
Si se utiliza Docker:
```powershell
docker start sonarqube-local
# O si se monta por primera vez:
# docker run -d --name sonarqube-local -p 9000:9000 sonarqube:lts-community
```

> [!NOTE]
> Esperar aproximadamente 30 a 50 segundos a que el servidor inicialice. Se puede verificar su disponibilidad en: [http://localhost:9000](http://localhost:9000).

---

### Paso 2: Generar / Obtener el Token de Acceso

1. Ingresar en el navegador a: [http://localhost:9000](http://localhost:9000).
2. Ir al menú de usuario (esquina superior derecha) ➔ **My Account** ➔ pestaña **Security**.
3. En la sección **Generate Token**:
   * **Nombre:** `styleradar-token`
   * **Tipo:** `User Token` o `Global Analysis Token`
   * Clic en **Generate** y copiar el token generado (`squ_...`).

---

### Paso 3: Ejecutar el Análisis Maven desde la Raíz del Microservicio

Desde la raíz del repositorio (`PT8080-Donations-Service`), ejecutar:

```powershell
mvn clean verify sonar:sonar "-Dsonar.login=<TU_TOKEN_SQU>"
```

> [!IMPORTANT]
> Es fundamental ejecutar `clean verify` junto con `sonar:sonar`. Esto garantiza que los 167 tests se ejecuten y JaCoCo genere el reporte `target/site/jacoco/jacoco.xml` antes de que SonarQube escanee el proyecto, permitiéndole cargar los porcentajes de cobertura de líneas y ramas.
> En PowerShell, asegurarse de encerrar el argumento completo entre comillas: `"-Dsonar.login=squ_..."`.

---

## 4. Quality Gate y Métricas Oficiales Obtenidas

Al finalizar el escaneo, ingresar al dashboard:  
👉 **[http://localhost:9000/dashboard?id=styleradar_ms-segunda-vida-donaciones](http://localhost:9000/dashboard?id=styleradar_ms-segunda-vida-donaciones)**

| Métrica | Valor Obtenido | Umbral Exigido | Estado |
| :--- | :---: | :---: | :---: |
| **Quality Gate** | **PASSED (Verde)** | Passed | ✅ APROBADO |
| **Confiabilidad (Reliability)** | **A (0 Bugs)** | 0 Bugs | ✅ MÁXIMA CALIFICACIÓN |
| **Seguridad (Security)** | **A (0 Vulnerabilidades)** | 0 Vulnerabilidades | ✅ MÁXIMA CALIFICACIÓN |
| **Revisión de Seguridad (Security Review)** | **A (0 Hotspots)** | 0 Hotspots pendientes | ✅ MÁXIMA CALIFICACIÓN |
| **Mantenibilidad (Maintainability)** | **A (Deuda: 0.3%)** | Deuda < 5% | ✅ MÁXIMA CALIFICACIÓN |
| **Duplicación de Código** | **0.0%** | < 3.0% | ✅ MÁXIMA CALIFICACIÓN |
| **Cobertura de Pruebas (JaCoCo XML)** | **96.3%** | ≥ 85.0% | ✅ SUPERA META (167 Tests) |

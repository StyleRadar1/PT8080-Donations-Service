[← Anterior: 5. Tecnologías y Dependencias](./05-tecnologias-y-dependencias.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Variables de Entorno →](./07-variables-de-entorno.md)

# 6. Cómo Ejecutar Local y con Docker

## Prerrequisitos de Entorno

* **Java Development Kit (JDK):** Versión 21 LTS instalada (`java -version`).
* **Maven Wrapper (`./mvnw`):** Incluido en el repositorio (Apache Maven 3.9.16 integrado; opcionalmente `mvn` 3.9+).
* **Docker Engine & Docker Compose (Opcional):** Versión 24+ en ejecución (`docker --version`).

---

## Opción 1: Ejecución Instantánea Local con Perfil H2 (`local` por defecto, sin dependencias externas)

No requiere levantar Docker ni bases de datos externas; `spring.profiles.default: local` activa automáticamente el perfil `local` con H2 y el secreto JWT de desarrollo:

```powershell
./mvnw spring-boot:run
```

* **Swagger UI:** [http://localhost:8520/api/v1/swagger-ui.html](http://localhost:8520/api/v1/swagger-ui.html)
* **Health Check:** [http://localhost:8520/api/v1/health](http://localhost:8520/api/v1/health)

---

## Opción 2: Ejecución Contenerizada con Docker Compose (PostgreSQL 16 + RabbitMQ)

```bash
# 1. Clonar el repositorio
git clone https://github.com/StyleRadar1/PT8080-Donations-Service.git
cd PT8080-Donations-Service

# 2. Levantar la suite completa
docker compose up -d --build

# 3. Verificar estado saludable
docker compose ps
```

---

## Opción 3: Compilar y Verificar Pruebas + Cobertura JaCoCo en Local

```powershell
./mvnw -B clean verify "-Dapi.version=1.44"
```

---

[← Anterior: 5. Tecnologías y Dependencias](./05-tecnologias-y-dependencias.md) &nbsp;|&nbsp; [Índice Principal](../../README.md) &nbsp;|&nbsp; [Siguiente: 7. Variables de Entorno →](./07-variables-de-entorno.md)

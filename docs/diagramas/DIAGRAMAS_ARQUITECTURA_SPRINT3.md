# Diagramas de Arquitectura y Diseño — Sprint 3
## Microservicio `ms-segunda-vida-donaciones` (Puerto 8520) — StyleRadar

Este documento compila y documenta los 3 diagramas arquitectónicos oficiales del microservicio, actualizados con la evolución de dominio de economía circular (Vitrina Abierta C2C y Donaciones benéficas, restricción a 20 localidades de Bogotá D.C., concurrencia optimista y mensajería RabbitMQ AMQPS TLS).

---

## 1. Diagrama de Clases del Dominio y Puertos Hexagonales

El modelo de dominio es 100% puro (Java POJOs sin dependencias de frameworks ni JPA). El agregado `SecondHandPublication` unifica las publicaciones de segunda mano (`esDonacion = false`) y las de donación benéfica (`esDonacion = true`).

```mermaid
classDiagram
    direction TB

    class SecondHandPublication {
        -Long id
        -Long userId
        -Garment garment
        -Size size
        -ConservationStatus conservationStatus
        -BigDecimal price
        -boolean esDonacion
        -LocalidadBogota localidad
        -PublicationStatus status
        -Long version
        -List~String~ photos
        -LocalDateTime publishedAt
        -LocalDateTime updatedAt
        +validateInvariants() void
        +publish() void
        +edit(...) void
        +markAsSold() void
        +retire() void
        +markForDonation() void
        +markAsDonated() void
    }

    class Garment {
        <<Value Object>>
        -String name
        -String description
        -GarmentType garmentType
        -String brand
        -String color
        -Style style
        +validateInvariants() void
    }

    class DonationRequest {
        -Long id
        -Long publicationId
        -Long donorId
        -Long foundationId
        -String message
        -DonationRequestStatus status
        -String rejectionReason
        -String deliveryPoint
        -LocalDateTime pickupStartTime
        -LocalDateTime pickupEndTime
        -Long version
        -LocalDateTime requestedAt
        -LocalDateTime decidedAt
        -LocalDateTime receivedAt
        +initiate() void
        +approve() void
        +reject(String reason) void
        +schedulePickup(String point, LocalDateTime start, LocalDateTime end) void
        +confirmReceipt() void
    }

    class LocalidadBogota {
        <<Enumeration>>
        USAQUEN
        CHAPINERO
        SANTA_FE
        SAN_CRISTOBAL
        USME
        TUNJUELITO
        BOSA
        KENNEDY
        FONTIBON
        ENGATIVA
        SUBA
        BARRIOS_UNIDOS
        TEUSAQUILLO
        LOS_MARTIRES
        ANTONIO_NARINO
        PUENTE_ARANDA
        LA_CANDELARIA
        RAFAEL_URIBE_URIBE
        CIUDAD_BOLIVAR
        SUMAPAZ
    }

    class PublicationStatus {
        <<Enumeration>>
        AVAILABLE
        SOLD
        IN_DONATION
        DONATED
        RETIRED
    }

    class DonationRequestStatus {
        <<Enumeration>>
        PENDING
        ACCEPTED
        REJECTED
        SCHEDULED
        DELIVERED
        CANCELLED
    }

    %% Puertos Hexagonales de Entrada
    class CreatePublicationUseCase {
        <<Interface / In Port>>
        +create(CreatePublicationCommand) SecondHandPublication
    }

    class ManagePublicationUseCase {
        <<Interface / In Port>>
        +edit(Long, EditPublicationCommand) SecondHandPublication
        +retire(Long, Long) void
        +markAsSold(Long, Long) void
        +markForDonation(Long, Long) void
    }

    class ClaimDonationUseCase {
        <<Interface / In Port>>
        +claimDonation(ClaimDonationCommand) DonationRequest
    }

    %% Puertos Hexagonales de Salida
    class PublicationRepositoryPort {
        <<Interface / Out Port>>
        +save(SecondHandPublication) SecondHandPublication
        +findById(Long) Optional~SecondHandPublication~
        +search(PublicationSearchCriteria) List~SecondHandPublication~
    }

    class DonationRequestRepositoryPort {
        <<Interface / Out Port>>
        +save(DonationRequest) DonationRequest
        +findById(Long) Optional~DonationRequest~
        +findByDonorId(Long) List~DonationRequest~
        +findByFoundationId(Long) List~DonationRequest~
        +existsActiveRequestForPublication(Long) boolean
    }

    class DonationEventPublisherPort {
        <<Interface / Out Port>>
        +publishEvent(Object) void
    }

    %% Relaciones
    SecondHandPublication *-- Garment : compone
    SecondHandPublication o-- LocalidadBogota : ubicada en
    SecondHandPublication o-- PublicationStatus : estado
    DonationRequest o-- DonationRequestStatus : estado
    SecondHandPublication "1" <-- "0..*" DonationRequest : referencia a
```

---

## 2. Diagrama Entidad-Relación (Base de Datos PostgreSQL `styleradar_donations`)

Modelo relacional en 3FN desplegado sobre PostgreSQL 16 con migraciones Flyway (`V1`, `V2`, `V3`). Incluye discriminador `es_donacion`, control de concurrencia optimista (`version`) y restricciones geográficas de Bogotá.

```mermaid
erDiagram
    PUBLICACIONES_SEGUNDA_MANO ||--o{ FOTOS_PUBLICACION : "posee (1 a 4)"
    PUBLICACIONES_SEGUNDA_MANO ||--o{ SOLICITUDES_DONACION : "recibe solicitudes benéficas"

    PUBLICACIONES_SEGUNDA_MANO {
        bigint id PK "GENERATED ALWAYS AS IDENTITY"
        bigint usuario_id "FK lógico -> ms-usuarios (Donante o Vendedor)"
        varchar_120 nombre_prenda "Nombre de la prenda"
        varchar_1000 descripcion "Detalle de uso y medidas"
        varchar_30 tipo_prenda "Enum: UPPER_BODY, LOWER_BODY, FOOTWEAR..."
        varchar_80 marca "Marca original"
        varchar_50 color "Color primario"
        varchar_30 estilo "Enum: CASUAL, FORMAL, SPORT, VINTAGE..."
        varchar_20 talla "Enum: XS, S, M, L, XL, XXL..."
        varchar_30 estado_conservacion "Enum: LIKE_NEW, GOOD, FAIR"
        numeric_12_2 precio "0 COP para donación, > 0 COP para reventa"
        boolean es_donacion "Discriminador: TRUE=Donación, FALSE=Segunda Mano"
        varchar_50 localidad "Enum: 20 Localidades oficiales de Bogotá D.C."
        varchar_30 estado "Enum: AVAILABLE, SOLD, IN_DONATION, DONATED, RETIRED"
        bigint version "Control de Concurrencia Optimista (@Version)"
        timestamp_tz fecha_publicacion "Fecha de alta en vitrina"
        timestamp_tz fecha_actualizacion "Última modificación"
    }

    FOTOS_PUBLICACION {
        bigint id PK "GENERATED ALWAYS AS IDENTITY"
        bigint publicacion_id FK "REFERENCES publicaciones_segunda_mano(id) ON DELETE CASCADE"
        varchar_512 url_foto "URL segura HTTPS hacia S3 / Blob Storage"
        integer orden "Índice de orden 0..3"
    }

    SOLICITUDES_DONACION {
        bigint id PK "GENERATED ALWAYS AS IDENTITY"
        bigint publicacion_id FK "REFERENCES publicaciones_segunda_mano(id)"
        bigint donante_id "FK lógico -> ms-usuarios (Usuario, Tienda o Fundación)"
        bigint fundacion_id "FK lógico -> ms-usuarios (Fundación receptora)"
        varchar_500 mensaje "Mensaje de motivación o necesidad"
        varchar_30 estado "Enum: PENDING, ACCEPTED, REJECTED, SCHEDULED, DELIVERED"
        varchar_500 motivo_rechazo "Motivo si fue declinada"
        varchar_255 punto_entrega "Sede de entrega coordinada en Bogotá"
        timestamp_tz franja_inicio "Inicio de ventana horaria de recogida"
        timestamp_tz franja_fin "Fin de ventana horaria de recogida"
        bigint version "Control de Concurrencia Optimista (@Version)"
        timestamp_tz fecha_solicitud "Fecha de postulación o reclamo"
        timestamp_tz fecha_decision "Fecha de aprobación/rechazo"
        timestamp_tz fecha_recepcion "Fecha de confirmación de entrega"
    }
```

---

## 3. Diagrama de Componentes C4 (Nivel 3 — Arquitectura Hexagonal)

Muestra los adaptadores de entrada (REST API Controllers, filtros JWT/RBAC), los casos de uso de la capa de aplicación, el modelo de dominio puro, y los adaptadores de salida (JPA Persistence Adapter y RabbitMQ AMQPS TLS Event Publisher Adapter hacia el broker central `styleradar.exchange`).

```mermaid
flowchart TD
    subgraph External_Actors ["Actores y Clientes Externos"]
        ClientWeb["StyleRadar Web SPA / Mobile App"]
        MsChat["ms-chat (Microservicio de Chat)"]
        MsNotif["ms-notificaciones (Microservicio de Notificaciones)"]
        MsUsers["ms-usuarios (Microservicio de Identidad)"]
    end

    subgraph MS_Boundary ["ms-segunda-vida-donaciones (Puerto 8520)"]
        subgraph Primary_Adapters ["Adaptadores de Entrada (Driver Adapters)"]
            SecFilter["Security & JWT Auth Filter\n(Roles: USER, FOUNDATION, STORE, ADMIN)"]
            PubCtrl["SecondHandPublicationController\n(/api/v1/publications)"]
            DonCtrl["DonationRequestController\n(/api/v1/donations)"]
            AuthCtrl["AuthController\n(/api/v1/auth/token)"]
            HealthCtrl["HealthController\n(/api/v1/health)"]
        end

        subgraph Application_Layer ["Capa de Aplicación (Use Cases & DTOs)"]
            UC_CreatePub["CreatePublicationUseCaseImpl"]
            UC_ManagePub["ManagePublicationUseCaseImpl"]
            UC_GetPub["GetPublicationsUseCaseImpl"]
            UC_CreateDon["CreateDonationRequestUseCaseImpl"]
            UC_ClaimDon["ClaimDonationUseCaseImpl (Vitrina Abierta)"]
            UC_DecideDon["DecideDonationRequestUseCaseImpl"]
            UC_ScheduleDon["ScheduleDonationPickupUseCaseImpl"]
            UC_ConfirmDon["ConfirmDonationReceiptUseCaseImpl"]
        end

        subgraph Domain_Layer ["Capa de Dominio Puro (Domain Core)"]
            Aggr_Pub["SecondHandPublication (Agregado Raíz)\n[esDonacion, localidad, version]"]
            Aggr_Don["DonationRequest (Entidad)\n[DonationRequestStatus, version]"]
            VO_Garment["Garment (Value Object)"]
            Enum_Loc["LocalidadBogota (20 Localidades)"]
            DomEvents["Domain Events:\n- PublicationSoldEvent\n- DonationRequestCreatedEvent"]
        end

        subgraph Secondary_Adapters ["Adaptadores de Salida (Driven Adapters)"]
            JPA_PubAdapter["PublicationPersistenceAdapter\n(Spring Data JPA Repository)"]
            JPA_DonAdapter["DonationRequestPersistenceAdapter\n(Spring Data JPA Repository)"]
            Rabbit_Publisher["DonationEventPublisherAdapter\n(RabbitTemplate - AMQPS TLS 5671)"]
        end
    end

    subgraph External_Infrastructure ["Infraestructura y Broker Externo"]
        DB[(PostgreSQL 16\nstyleradar_donations\nPuerto 5434/5432)]
        RabbitBroker["RabbitMQ Broker (Azure QA)\nstyleradar-rabbitmq-qa:5671 TLS\nExchange: styleradar.exchange"]
        DLX["Dead Letter Exchange\nstyleradar.dlx -> dlq"]
    end

    %% Flujos de interacción
    ClientWeb -->|HTTPS REST + Bearer JWT| SecFilter
    SecFilter --> PubCtrl
    SecFilter --> DonCtrl
    SecFilter --> AuthCtrl
    SecFilter --> HealthCtrl

    PubCtrl --> UC_CreatePub
    PubCtrl --> UC_ManagePub
    PubCtrl --> UC_GetPub
    DonCtrl --> UC_CreateDon
    DonCtrl --> UC_ClaimDon
    DonCtrl --> UC_DecideDon
    DonCtrl --> UC_ScheduleDon
    DonCtrl --> UC_ConfirmDon

    Application_Layer --> Domain_Layer

    UC_CreatePub --> JPA_PubAdapter
    UC_ManagePub --> JPA_PubAdapter
    UC_ManagePub --> Rabbit_Publisher
    UC_GetPub --> JPA_PubAdapter

    UC_CreateDon --> JPA_DonAdapter
    UC_CreateDon --> JPA_PubAdapter
    UC_CreateDon --> Rabbit_Publisher
    UC_ClaimDon --> JPA_DonAdapter
    UC_ClaimDon --> JPA_PubAdapter
    UC_ClaimDon --> Rabbit_Publisher
    UC_DecideDon --> JPA_DonAdapter
    UC_DecideDon --> Rabbit_Publisher
    UC_ScheduleDon --> JPA_DonAdapter
    UC_ScheduleDon --> Rabbit_Publisher
    UC_ConfirmDon --> JPA_DonAdapter
    UC_ConfirmDon --> JPA_PubAdapter
    UC_ConfirmDon --> Rabbit_Publisher

    JPA_PubAdapter -->|JDBC SQL Pooling| DB
    JPA_DonAdapter -->|JDBC SQL Pooling| DB

    Rabbit_Publisher -->|AMQPS TLS: styleradar.segundavida.prenda.vendida| RabbitBroker
    Rabbit_Publisher -->|AMQPS TLS: styleradar.donaciones.estado.cambiado| RabbitBroker
    RabbitBroker -.->|DLX on failure| DLX

    RabbitBroker -->|q.chat.segunda-mano| MsChat
    RabbitBroker -->|q.notificaciones.*| MsNotif
```

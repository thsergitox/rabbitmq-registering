# LP1 - Persistencia Java

Este directorio contiene la aplicación Java que actúa como **LP1** en el sistema distribuido. Su función principal es persistir los datos de usuarios en la **BD1** usando PostgreSQL.

## Funcionalidad

- **Persistencia de Usuarios**: Guarda los datos del usuario en la BD1
- **Gestión de Amigos**: Maneja la tabla de relaciones de amistad
- **Comunicación con RabbitMQ**: Consume mensajes de `queue_lp1` y publica confirmaciones

## Esquema de BD1

```sql
CREATE TABLE users (
    id INT,
    nombre VARCHAR(512),
    correo VARCHAR(512),
    clave INT,
    dni INT,
    telefono INT
);

CREATE TABLE friend (
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Flujo de Mensajes

1. **Recibe**: `lp1.persist` → Datos validados para persistir
2. **Envía**: `lp1.persisted` → Confirmación de persistencia exitosa

## Configuración

### Variables de Entorno
- `DB_HOST`: Host de PostgreSQL (default: postgres)
- `DB_PORT`: Puerto de PostgreSQL (default: 5432)
- `DB_NAME`: Nombre de la base de datos (default: bd1_users)
- `DB_USER`: Usuario de la base de datos (default: lp1_user)
- `DB_PASSWORD`: Contraseña de la base de datos (default: lp1_pass)
- `RABBITMQ_HOST`: Host de RabbitMQ (default: rabbitmq)
- `RABBITMQ_PORT`: Puerto de RabbitMQ (default: 5672)
- `RABBITMQ_USER`: Usuario de RabbitMQ (default: admin)
- `RABBITMQ_PASSWORD`: Contraseña de RabbitMQ (default: admin123)

## Cómo ejecutar

```bash
cd java
docker-compose up -d
```

## Estructura del Proyecto

```
java/
├── docker-compose.yml    # Configuración de contenedores
├── pom.xml              # Configuración Maven
├── db.sql               # Esquema e datos iniciales BD1
├── src/                 # Código fuente de la aplicación
│   ├── Main.java        # Aplicación principal
│   ├── DatabaseManager.java # Gestión de base de datos
│   ├── RabbitMQClient.java   # Cliente RabbitMQ
│   └── UserModel.java   # Modelo de datos de usuario
└── README.md           # Este archivo
```

## Tecnologías

- **Java 21**: Lenguaje de programación
- **Maven**: Gestión de dependencias
- **PostgreSQL**: Base de datos
- **RabbitMQ**: Sistema de mensajería
- **HikariCP**: Pool de conexiones
- **GSON**: Procesamiento JSON

## Puerto

- **Aplicación**: 8081
- **PostgreSQL**: 5432 
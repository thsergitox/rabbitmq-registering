# LP2 - Validador Python

Este directorio contiene la aplicación Python que actúa como **LP2** en el sistema distribuido. Su función principal es validar datos contra la **BD2 (DNI)** usando MariaDB.

## Funcionalidad

- **Validación de DNI**: Verifica si el DNI existe en la BD2
- **Validación de Amigos**: Confirma que los amigos especificados existen en la BD2
- **Comunicación con RabbitMQ**: Consume mensajes de `queue_lp2` y publica resultados

## Esquema de BD2

```sql
CREATE TABLE persona (
    id INT,
    dni INT,
    nombre VARCHAR(512),
    apellidos VARCHAR(512),
    lugar_nac VARCHAR(512),
    ubigeo INT,
    direccion VARCHAR(512)
);
```

## Flujo de Mensajes

1. **Recibe**: `lp2.validate` → Validar usuario y amigos
2. **Envía**: 
   - `lp2.query.ok` → Si toda la validación es exitosa
   - `lp2.query.fail` → Si falla alguna validación

## Configuración

### Variables de Entorno
- `DB_HOST`: Host de MariaDB (default: mariadb)
- `DB_PORT`: Puerto de MariaDB (default: 3306)  
- `DB_NAME`: Nombre de la base de datos (default: bd2_dni)
- `DB_USER`: Usuario de la base de datos (default: lp2_user)
- `DB_PASSWORD`: Contraseña de la base de datos (default: lp2_pass)
- `RABBITMQ_HOST`: Host de RabbitMQ (default: rabbitmq)
- `RABBITMQ_PORT`: Puerto de RabbitMQ (default: 5672)
- `RABBITMQ_USER`: Usuario de RabbitMQ (default: admin)
- `RABBITMQ_PASSWORD`: Contraseña de RabbitMQ (default: admin123)

## Cómo ejecutar

```bash
cd python
docker-compose up -d
```

## Estructura del Proyecto

```
python/
├── docker-compose.yml    # Configuración de contenedores
├── requirements.txt      # Dependencias Python
├── db.sql               # Esquema e datos iniciales BD2
├── src/                 # Código fuente de la aplicación
│   ├── main.py          # Aplicación principal
│   ├── db_manager.py    # Gestión de base de datos
│   └── rabbitmq_client.py # Cliente RabbitMQ
└── README.md           # Este archivo
```

## Puerto

- **Aplicación**: 8082
- **MariaDB**: 3306 
# RabbitMQ Configuration

Este directorio contiene la configuración para el middleware RabbitMQ que orquesta la comunicación entre LP1 (Java), LP2 (Python) y LP3 (Node.js).

## Cómo levantar el servicio

```bash
cd rabbitmq
docker-compose up -d
```

## Acceso

- **AMQP Port**: `5672`
- **Management UI**: `http://localhost:15672`
- **Credenciales**: 
  - Usuario: `admin`
  - Contraseña: `admin123`

## Arquitectura de Messaging

### Exchange
- **Nombre**: `registro_bus`
- **Tipo**: `direct`

### Colas y Routing Keys

| Cola | Routing Key | Destinatario | Propósito |
|------|-------------|--------------|-----------|
| `queue_lp2` | `lp2.validate` | LP2 (Python) | Recibe solicitudes de validación de DNI |
| `queue_lp1` | `lp1.persist` | LP1 (Java) | Recibe datos validados para persistir |
| `queue_lp3_ack` | `lp3.ack` | LP3 (Node.js) | Recibe confirmaciones del proceso |

### Flujo de Mensajes

1. **LP3** → `lp3.signup` → **RabbitMQ**
2. **RabbitMQ** → `lp2.validate` → **LP2**
3. **LP2** → `lp2.query.ok` / `lp2.query.fail` → **RabbitMQ**
4. Si OK: **RabbitMQ** → `lp1.persist` → **LP1**
5. **LP1** → `lp1.persisted` → **RabbitMQ**
6. **RabbitMQ** → `lp3.ack` → **LP3**

## Configuración automática

El archivo `definitions.json` configura automáticamente:
- Exchange `registro_bus`
- Las tres colas necesarias
- Los bindings con las routing keys correspondientes

## Monitoreo

Puedes monitorear las colas, exchanges y el flujo de mensajes a través del Management UI en `http://localhost:15672`. 
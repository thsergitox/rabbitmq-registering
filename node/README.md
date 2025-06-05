# LP3 - Cliente Node.js

Este directorio contiene la aplicación Node.js que actúa como **LP3** en el sistema distribuido. Su función principal es simular clientes que envían solicitudes de registro y evaluar el desempeño del sistema.

## Funcionalidad

- **Registro Individual**: Permite ingresar usuarios uno por uno de forma manual
- **Simulación Masiva**: Genera y envía 1000 registros aleatorios para evaluación de desempeño
- **Evaluación de Desempeño**: Mide tiempos de respuesta, throughput y tasa de éxito
- **Comunicación con RabbitMQ**: Publica solicitudes `lp3.signup` y consume confirmaciones `lp3.ack`

## Flujo de Mensajes

1. **Envía**: `lp3.signup` → Solicitud de registro de usuario
2. **Recibe**: `lp3.ack` → Confirmación del resultado (éxito/fallo)

## Modos de Operación

### 1. Modo Manual
Permite ingresar datos de usuario interactivamente:
- Nombre
- Correo
- Clave
- DNI
- Teléfono
- Amigos frecuentes (IDs separados por comas)

### 2. Modo Simulación
Genera automáticamente 1000 usuarios con datos aleatorios y mide:
- **Latencia promedio**: Tiempo de respuesta por solicitud
- **Throughput**: Solicitudes procesadas por segundo
- **Tasa de éxito**: Porcentaje de registros exitosos vs fallidos
- **Concurrencia**: Manejo de múltiples solicitudes simultáneas

## Configuración

### Variables de Entorno
- `PORT`: Puerto de la aplicación (default: 8083)
- `RABBITMQ_HOST`: Host de RabbitMQ (default: rabbitmq)
- `RABBITMQ_PORT`: Puerto de RabbitMQ (default: 5672)
- `RABBITMQ_USER`: Usuario de RabbitMQ (default: admin)
- `RABBITMQ_PASSWORD`: Contraseña de RabbitMQ (default: admin123)

## Cómo ejecutar

```bash
cd node
docker-compose up -d
```

O para desarrollo local:
```bash
cd node
npm install
npm start
```

## Estructura del Proyecto

```
node/
├── docker-compose.yml    # Configuración de contenedores
├── package.json         # Dependencias y scripts Node.js
├── app.js              # Aplicación principal
├── client.js           # Cliente RabbitMQ
├── simulator.js        # Simulador de carga
├── performance.js      # Métricas de desempeño
└── README.md          # Este archivo
```

## Scripts Disponibles

- `npm start`: Ejecuta la aplicación principal
- `npm run dev`: Ejecuta en modo desarrollo con nodemon
- `npm test`: Ejecuta el simulador de 1000 registros

## Tecnologías

- **Node.js 20**: Runtime de JavaScript
- **Express**: Framework web
- **amqplib**: Cliente RabbitMQ
- **inquirer**: Interfaz de línea de comandos interactiva
- **cli-progress**: Barras de progreso para simulaciones
- **uuid**: Generación de IDs únicos

## Puerto

- **Aplicación**: 8083

## Ejemplos de Uso

### Registro Manual
```bash
# Dentro del contenedor o localmente
node app.js --mode manual
```

### Simulación de Desempeño
```bash
# Dentro del contenedor o localmente
node app.js --mode simulation --count 1000
``` 
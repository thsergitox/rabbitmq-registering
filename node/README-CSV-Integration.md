# Integración con datos CSV - LP3 Cliente Node.js

## Resumen de cambios

Se ha modificado el cliente Node.js (`app.js` y `client.js`) para utilizar los datos reales del archivo `user.csv` en lugar de generar datos de prueba aleatorios para las simulaciones.

## Archivos modificados

### 1. `csvDataLoader.js` (NUEVO)
- **Función**: Cargador y gestor de datos CSV
- **Características**:
  - Carga automática del archivo `user.csv`
  - Formateo correcto de DNIs (8 dígitos)
  - Generación de amigos frecuentes aleatorios
  - Iteración cíclica sobre los usuarios
  - Métodos para obtener usuarios específicos o aleatorios

### 2. `app.js` (MODIFICADO)
- **Cambios principales**:
  - Importación del `CSVDataLoader`
  - Inicialización automática de datos CSV al iniciar
  - Nueva opción de menú: "Registro desde CSV (1 usuario)"
  - Simulación modificada para usar datos CSV reales
  - Configuración flexible del número de registros a simular
  - Opción para generar amigos frecuentes aleatorios
  - Nuevos endpoints web para estadísticas CSV
  - Función de prueba de conexión mejorada con datos CSV

### 3. `client.js` (SIN CAMBIOS DIRECTOS)
- El cliente RabbitMQ se mantiene igual
- Los datos ahora provienen del CSV a través del `app.js`

## Nuevas funcionalidades

### Menú principal actualizado:
1. 👤 Registro manual de usuario
2. 📊 Registro desde CSV (1 usuario) ← **NUEVO**
3. 🚀 Simulación con datos CSV (configurable) ← **MEJORADO**
4. 📈 Ver estadísticas
5. 🧪 Probar conexión
6. 📁 Ver información CSV ← **NUEVO**
7. ❌ Salir

### Simulación mejorada:
- Utiliza datos reales del CSV
- Permite configurar el número de registros (limitado por el CSV)
- Opción para generar amigos frecuentes aleatorios
- Control de concurrencia (1-50 solicitudes simultáneas)
- Estadísticas detalladas con información CSV

### Nuevos endpoints API:
- `GET /health` - Incluye información sobre datos CSV cargados
- `GET /stats` - Estadísticas de performance y CSV
- `GET /random-user` - Obtiene un usuario aleatorio del CSV

## Estructura de datos CSV

El archivo `user.csv` debe tener la siguiente estructura:
```
id,nombre,correo,clave,dni,telefono,timestamp
1,Miguel Castro,miguel.castro1@example.com,3281,58889613,963789355,2025-06-05 17:45:53.579949 +00:00
```

### Campos:
- **id**: Identificador único del usuario
- **nombre**: Nombre completo
- **correo**: Dirección de email
- **clave**: Clave de 4 dígitos
- **dni**: DNI (se formatea automáticamente a 8 dígitos)
- **telefono**: Número de teléfono
- **timestamp**: Marca de tiempo (opcional)

## Uso

### Requisitos:
1. El archivo `user.csv` debe estar en el directorio `node/`
2. Las dependencias de Node.js deben estar instaladas (`npm install`)

### Ejecutar:
```bash
cd node/
npm start
# o
node app.js
```

### Simulación con datos CSV:
1. Seleccionar opción "Simulación con datos CSV"
2. Configurar número de registros (máximo: cantidad disponible en CSV)
3. Configurar concurrencia (1-50)
4. Elegir si generar amigos frecuentes aleatorios
5. Observar el progreso en tiempo real

## Características técnicas

### Gestión de datos:
- **Carga automática**: El CSV se carga automáticamente al iniciar
- **Validación**: Se filtran registros inválidos o incompletos
- **Iteración cíclica**: Los usuarios se reutilizan cuando se agotan
- **Amigos frecuentes**: Se generan automáticamente de 0-3 amigos por usuario

### Performance:
- **Concurrencia controlada**: Limita las solicitudes simultáneas
- **Métricas detalladas**: Latencia, throughput, tasa de éxito
- **Progreso visual**: Barra de progreso en tiempo real

### Robustez:
- **Manejo de errores**: Continúa funcionando sin CSV
- **Fallback**: Usa datos de prueba si no hay CSV disponible
- **Validación**: Formatos correctos para DNI, teléfono, etc.

## Estadísticas disponibles

### Performance:
- Solicitudes totales, exitosas y fallidas
- Tasa de éxito
- Throughput (req/seg)
- Latencia promedio, mínima y máxima

### Datos CSV:
- Usuarios cargados
- Índice actual de iteración
- Ruta del archivo CSV
- Estado de carga

## Ejemplo de ejecución

```
🏁 Iniciando LP3 - Cliente Node.js
📊 Cargando datos de usuarios...
📁 Cargando datos de usuarios desde user.csv...
✅ Cargados 305 usuarios desde CSV
✅ Datos CSV cargados: 305 usuarios disponibles
✅ Conectado a RabbitMQ en 192.168.18.72:5672
✅ Exchange y colas verificados correctamente
✅ LP3 Cliente conectado a RabbitMQ
✅ Consumidor configurado para cola: queue_lp3_ack
🌐 Servidor web LP3 ejecutándose en puerto 8083
```

## Notas importantes

1. **Archivo CSV**: Debe estar presente en `node/user.csv`
2. **Formato**: CSV sin cabeceras, campos separados por comas
3. **Encoding**: UTF-8 recomendado para caracteres especiales
4. **Tamaño**: El sistema maneja eficientemente archivos grandes
5. **Reutilización**: Los datos se reutilizan cíclicamente en simulaciones grandes 
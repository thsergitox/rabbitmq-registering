# Node.js Registration Client (LP3)

## Overview
Express.js client service that provides REST API for user registration through RabbitMQ.

## Quick Start

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the service:
   ```bash
   npm start
   ```

3. Development mode (with auto-reload):
   ```bash
   npm run dev
   ```

## API Endpoints

- `GET /` - Service info
- `GET /api/health` - Basic health check
- `GET /api/health/detailed` - Detailed health status
- `POST /api/register` - Register a new user
- `GET /api/register/status` - Registration service status

## Registration Request Format

```json
{
  "nombre": "John Doe",
  "correo": "john@example.com",
  "clave": 123456,
  "dni": 12345678,
  "telefono": 987654321,
  "friendsDni": [87654321, 98765432]
}
```

## Performance Testing

Run performance test with 1000 users:
```bash
npm run perf-test
```

## Testing

Run unit tests:
```bash
npm test
```

## Configuration

Configuration is loaded from `.env` file. See `.env.example` for available options.

## Requirements

- Node.js 20+
- RabbitMQ running on localhost:5672
- Java service (LP1) and Python service (LP2) for complete flow 
import { Router } from 'express';
import { rabbitmqService } from '../services/rabbitmq.js';

const router = Router();

// Basic health check
router.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    service: 'registration-client',
  });
});

// Detailed health check
router.get('/health/detailed', (req, res) => {
  const rabbitmqConnected = rabbitmqService.isConnected();
  
  const health = {
    status: rabbitmqConnected ? 'healthy' : 'unhealthy',
    timestamp: new Date().toISOString(),
    service: 'registration-client',
    checks: {
      rabbitmq: {
        status: rabbitmqConnected ? 'up' : 'down',
        connected: rabbitmqConnected,
      },
    },
    environment: process.env.NODE_ENV,
    uptime: process.uptime(),
  };
  
  const statusCode = health.status === 'healthy' ? 200 : 503;
  res.status(statusCode).json(health);
});

export default router; 
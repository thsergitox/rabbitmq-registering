import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';
import { config } from './config/config.js';
import { logger } from './utils/logger.js';
import { rabbitmqService } from './services/rabbitmq.js';
import healthRoutes from './routes/health.js';
import registerRoutes from './routes/register.js';

// Create Express app
const app = express();

// Apply middleware
app.use(helmet()); // Security headers
app.use(cors(config.cors)); // CORS
app.use(express.json()); // JSON body parser
app.use(express.urlencoded({ extended: true })); // URL-encoded body parser

// Request logging
if (config.server.nodeEnv !== 'test') {
  app.use(morgan('combined', {
    stream: { write: (message) => logger.info(message.trim()) },
  }));
}

// Rate limiting
const limiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.maxRequests,
  message: 'Too many requests from this IP, please try again later.',
});
app.use('/api/', limiter);

// Routes
app.use('/api', healthRoutes);
app.use('/api', registerRoutes);

// Root endpoint
app.get('/', (req, res) => {
  res.json({
    service: config.app.name,
    version: '1.0.0',
    status: 'running',
    endpoints: {
      health: '/api/health',
      healthDetailed: '/api/health/detailed',
      register: '/api/register',
      registerStatus: '/api/register/status',
    },
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not found',
    path: req.path,
  });
});

// Error handler
app.use((err, req, res, next) => {
  logger.error('Unhandled error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: config.server.nodeEnv === 'development' ? err.message : undefined,
  });
});

// Start server
async function startServer() {
  try {
    // Connect to RabbitMQ
    await rabbitmqService.connect();
    
    // Start Express server
    const server = app.listen(config.server.port, () => {
      logger.info(`Server running on port ${config.server.port}`);
      logger.info(`Environment: ${config.server.nodeEnv}`);
    });
    
    // Graceful shutdown
    process.on('SIGTERM', async () => {
      logger.info('SIGTERM signal received: closing HTTP server');
      server.close(async () => {
        logger.info('HTTP server closed');
        await rabbitmqService.close();
        process.exit(0);
      });
    });
    
    process.on('SIGINT', async () => {
      logger.info('SIGINT signal received: closing HTTP server');
      server.close(async () => {
        logger.info('HTTP server closed');
        await rabbitmqService.close();
        process.exit(0);
      });
    });
    
  } catch (error) {
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Start the server
startServer();

export default app; 
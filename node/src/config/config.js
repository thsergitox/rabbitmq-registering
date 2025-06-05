import dotenv from 'dotenv';

// Load environment variables
dotenv.config();

export const config = {
  server: {
    port: parseInt(process.env.PORT || '8083', 10),
    nodeEnv: process.env.NODE_ENV || 'development',
    requestTimeout: parseInt(process.env.REQUEST_TIMEOUT || '30000', 10),
  },
  rabbitmq: {
    host: process.env.RABBITMQ_HOST || 'localhost',
    port: parseInt(process.env.RABBITMQ_PORT || '5672', 10),
    username: process.env.RABBITMQ_USERNAME || 'admin',
    password: process.env.RABBITMQ_PASSWORD || 'admin123',
    vhost: process.env.RABBITMQ_VHOST || '/',
    // Exchange and queue configuration
    exchange: 'registro_bus',
    queues: {
      signup: 'queue_lp3',
      ack: 'queue_lp3_ack',
    },
    routingKeys: {
      signup: 'lp3.signup',
      persisted: 'lp1.persisted',
    },
  },
  app: {
    name: process.env.APP_NAME || 'registration-client',
    logLevel: process.env.LOG_LEVEL || 'info',
  },
  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10),
    maxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '100', 10),
  },
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
  },
  performanceTest: {
    users: parseInt(process.env.PERF_TEST_USERS || '1000', 10),
    batchSize: parseInt(process.env.PERF_TEST_BATCH_SIZE || '50', 10),
    delayMs: parseInt(process.env.PERF_TEST_DELAY_MS || '100', 10),
  },
};

export default config; 
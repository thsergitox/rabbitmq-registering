import axios from 'axios';
import { config } from './config/config.js';
import { logger } from './utils/logger.js';

// Performance test configuration
const TOTAL_USERS = config.performanceTest.users;
const BATCH_SIZE = config.performanceTest.batchSize;
const DELAY_BETWEEN_BATCHES = config.performanceTest.delayMs;
const API_URL = `http://localhost:${config.server.port}/api/register`;

// Metrics collector
class MetricsCollector {
  constructor() {
    this.results = [];
    this.startTime = null;
    this.endTime = null;
  }

  start() {
    this.startTime = Date.now();
    logger.info(`Performance test started at ${new Date(this.startTime).toISOString()}`);
  }

  recordResult(result) {
    this.results.push(result);
  }

  finish() {
    this.endTime = Date.now();
    const duration = (this.endTime - this.startTime) / 1000;
    
    const successful = this.results.filter(r => r.success).length;
    const failed = this.results.filter(r => !r.success).length;
    const responseTimes = this.results.map(r => r.responseTime).sort((a, b) => a - b);
    
    const metrics = {
      totalRequests: this.results.length,
      successful,
      failed,
      successRate: ((successful / this.results.length) * 100).toFixed(2) + '%',
      totalDuration: duration.toFixed(2) + 's',
      requestsPerSecond: (this.results.length / duration).toFixed(2),
      responseTimeMetrics: {
        min: Math.min(...responseTimes),
        max: Math.max(...responseTimes),
        avg: (responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length).toFixed(2),
        p50: responseTimes[Math.floor(responseTimes.length * 0.5)],
        p95: responseTimes[Math.floor(responseTimes.length * 0.95)],
        p99: responseTimes[Math.floor(responseTimes.length * 0.99)],
      },
    };
    
    return metrics;
  }
}

// Generate random user data
function generateUserData(index) {
  const dni = 10000000 + index;
  return {
    nombre: `Test User ${index}`,
    correo: `testuser${index}@example.com`,
    clave: 100000 + (index % 900000),
    dni: dni,
    telefono: 900000000 + index,
    friendsDni: index > 10 ? [dni - 1, dni - 2] : [],
  };
}

// Send registration request
async function sendRegistrationRequest(userData) {
  const startTime = Date.now();
  
  try {
    const response = await axios.post(API_URL, userData, {
      timeout: config.server.requestTimeout,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    return {
      success: response.data.success,
      responseTime: Date.now() - startTime,
      statusCode: response.status,
      dni: userData.dni,
    };
  } catch (error) {
    return {
      success: false,
      responseTime: Date.now() - startTime,
      statusCode: error.response?.status || 0,
      dni: userData.dni,
      error: error.message,
    };
  }
}

// Process batch of users
async function processBatch(startIndex, batchSize, metrics) {
  const promises = [];
  
  for (let i = 0; i < batchSize && startIndex + i < TOTAL_USERS; i++) {
    const userData = generateUserData(startIndex + i);
    promises.push(sendRegistrationRequest(userData));
  }
  
  const results = await Promise.allSettled(promises);
  
  results.forEach((result) => {
    if (result.status === 'fulfilled') {
      metrics.recordResult(result.value);
    } else {
      metrics.recordResult({
        success: false,
        responseTime: 0,
        statusCode: 0,
        error: result.reason,
      });
    }
  });
  
  const processed = startIndex + batchSize;
  const progress = ((processed / TOTAL_USERS) * 100).toFixed(1);
  logger.info(`Progress: ${progress}% (${processed}/${TOTAL_USERS} users)`);
}

// Main performance test function
async function runPerformanceTest() {
  logger.info('=== Performance Test Configuration ===');
  logger.info(`Total users: ${TOTAL_USERS}`);
  logger.info(`Batch size: ${BATCH_SIZE}`);
  logger.info(`Delay between batches: ${DELAY_BETWEEN_BATCHES}ms`);
  logger.info(`API URL: ${API_URL}`);
  logger.info('=====================================\n');
  
  // Wait for user confirmation
  logger.info('Starting performance test in 5 seconds...');
  await new Promise(resolve => setTimeout(resolve, 5000));
  
  const metrics = new MetricsCollector();
  metrics.start();
  
  try {
    // Process users in batches
    for (let i = 0; i < TOTAL_USERS; i += BATCH_SIZE) {
      await processBatch(i, BATCH_SIZE, metrics);
      
      // Delay between batches (except for the last batch)
      if (i + BATCH_SIZE < TOTAL_USERS) {
        await new Promise(resolve => setTimeout(resolve, DELAY_BETWEEN_BATCHES));
      }
    }
    
    // Calculate and display metrics
    const finalMetrics = metrics.finish();
    
    logger.info('\n=== Performance Test Results ===');
    logger.info(`Total Requests: ${finalMetrics.totalRequests}`);
    logger.info(`Successful: ${finalMetrics.successful}`);
    logger.info(`Failed: ${finalMetrics.failed}`);
    logger.info(`Success Rate: ${finalMetrics.successRate}`);
    logger.info(`Total Duration: ${finalMetrics.totalDuration}`);
    logger.info(`Requests/Second: ${finalMetrics.requestsPerSecond}`);
    logger.info('\n--- Response Time Metrics (ms) ---');
    logger.info(`Min: ${finalMetrics.responseTimeMetrics.min}ms`);
    logger.info(`Max: ${finalMetrics.responseTimeMetrics.max}ms`);
    logger.info(`Average: ${finalMetrics.responseTimeMetrics.avg}ms`);
    logger.info(`P50: ${finalMetrics.responseTimeMetrics.p50}ms`);
    logger.info(`P95: ${finalMetrics.responseTimeMetrics.p95}ms`);
    logger.info(`P99: ${finalMetrics.responseTimeMetrics.p99}ms`);
    logger.info('================================\n');
    
    // Check if performance meets requirements
    if (finalMetrics.responseTimeMetrics.p95 < 500) {
      logger.info('✅ Performance test PASSED - P95 < 500ms');
    } else {
      logger.warn('❌ Performance test FAILED - P95 >= 500ms');
    }
    
  } catch (error) {
    logger.error('Performance test failed:', error);
    process.exit(1);
  }
}

// Run the test
if (import.meta.url === `file://${process.argv[1]}`) {
  runPerformanceTest()
    .then(() => {
      logger.info('Performance test completed');
      process.exit(0);
    })
    .catch((error) => {
      logger.error('Performance test error:', error);
      process.exit(1);
    });
}

export default runPerformanceTest; 
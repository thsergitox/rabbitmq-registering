import { logger } from './logger.js';

/**
 * Retry configuration options
 * @typedef {Object} RetryOptions
 * @property {number} maxRetries - Maximum number of retry attempts
 * @property {number} initialDelay - Initial delay in ms
 * @property {number} maxDelay - Maximum delay in ms
 * @property {number} factor - Exponential backoff factor
 * @property {boolean} randomize - Add jitter to delay
 */

/**
 * Default retry options
 */
export const defaultRetryOptions = {
  maxRetries: 3,
  initialDelay: 1000,
  maxDelay: 30000,
  factor: 2,
  randomize: true,
};

/**
 * Calculate exponential backoff delay
 * @param {number} retryCount - Current retry attempt
 * @param {RetryOptions} options - Retry options
 * @returns {number} Delay in milliseconds
 */
function calculateDelay(retryCount, options) {
  let delay = options.initialDelay * Math.pow(options.factor, retryCount);
  
  // Cap at max delay
  delay = Math.min(delay, options.maxDelay);
  
  // Add jitter if enabled
  if (options.randomize) {
    const jitter = delay * 0.2 * Math.random();
    delay = delay + jitter;
  }
  
  return Math.floor(delay);
}

/**
 * Execute function with retry logic
 * @param {Function} fn - Function to execute
 * @param {RetryOptions} options - Retry options
 * @param {string} context - Context for logging
 * @returns {Promise<any>} Result of function execution
 */
export async function withRetry(fn, options = {}, context = 'operation') {
  const opts = { ...defaultRetryOptions, ...options };
  let lastError;
  
  for (let attempt = 0; attempt <= opts.maxRetries; attempt++) {
    try {
      const result = await fn();
      if (attempt > 0) {
        logger.info(`${context} succeeded after ${attempt} retries`);
      }
      return result;
    } catch (error) {
      lastError = error;
      
      if (attempt < opts.maxRetries) {
        const delay = calculateDelay(attempt, opts);
        logger.warn(`${context} failed (attempt ${attempt + 1}/${opts.maxRetries + 1}), retrying in ${delay}ms`, {
          error: error.message,
        });
        await new Promise(resolve => setTimeout(resolve, delay));
      } else {
        logger.error(`${context} failed after ${opts.maxRetries + 1} attempts`, {
          error: error.message,
        });
      }
    }
  }
  
  throw lastError;
}

/**
 * Check if error is retryable
 * @param {Error} error - Error to check
 * @returns {boolean} Whether error is retryable
 */
export function isRetryableError(error) {
  // Network errors
  if (error.code === 'ECONNREFUSED' || 
      error.code === 'ENOTFOUND' || 
      error.code === 'ETIMEDOUT' ||
      error.code === 'ECONNRESET') {
    return true;
  }
  
  // HTTP errors
  if (error.response) {
    const status = error.response.status;
    // Retry on 5xx errors and specific 4xx errors
    return status >= 500 || status === 429 || status === 408;
  }
  
  // RabbitMQ connection errors
  if (error.message && (
      error.message.includes('Connection closed') ||
      error.message.includes('Channel closed') ||
      error.message.includes('AMQP connection'))) {
    return true;
  }
  
  return false;
}

/**
 * Create a retry wrapper for a class method
 * @param {RetryOptions} options - Retry options
 * @returns {Function} Method decorator
 */
export function retryable(options = {}) {
  return function(target, propertyKey, descriptor) {
    const originalMethod = descriptor.value;
    
    descriptor.value = async function(...args) {
      const context = `${target.constructor.name}.${propertyKey}`;
      return withRetry(
        () => originalMethod.apply(this, args),
        options,
        context
      );
    };
    
    return descriptor;
  };
} 
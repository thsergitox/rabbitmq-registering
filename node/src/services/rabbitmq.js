import amqp from 'amqplib';
import { v4 as uuidv4 } from 'uuid';
import { config } from '../config/config.js';
import { rabbitmqLogger as logger } from '../utils/logger.js';

class RabbitMQService {
  constructor() {
    this.connection = null;
    this.channel = null;
    this.responseHandlers = new Map(); // Map to store response handlers by correlation ID
  }

  async connect() {
    try {
      const { host, port, username, password, vhost } = config.rabbitmq;
      const url = `amqp://${username}:${password}@${host}:${port}${vhost}`;
      
      logger.info('Connecting to RabbitMQ...');
      this.connection = await amqp.connect(url);
      this.channel = await this.connection.createChannel();
      
      // Setup exchange and queues
      await this.setupExchangeAndQueues();
      
      // Setup consumer for acknowledgments
      await this.setupConsumer();
      
      logger.info('Successfully connected to RabbitMQ');
      
      // Handle connection events
      this.connection.on('error', (err) => {
        logger.error('RabbitMQ connection error:', err);
      });
      
      this.connection.on('close', () => {
        logger.warn('RabbitMQ connection closed');
      });
      
    } catch (error) {
      logger.error('Failed to connect to RabbitMQ:', error);
      throw error;
    }
  }

  async setupExchangeAndQueues() {
    const { exchange, queues } = config.rabbitmq;
    
    // Declare exchange
    await this.channel.assertExchange(exchange, 'direct', { durable: true });
    
    // Declare queues
    await this.channel.assertQueue(queues.signup, { durable: true });
    await this.channel.assertQueue(queues.ack, { durable: true });
    
    // Bind queues
    await this.channel.bindQueue(queues.ack, exchange, 'lp1.persisted');
    
    logger.info('Exchange and queues configured');
  }

  async setupConsumer() {
    const { queues } = config.rabbitmq;
    
    // Set prefetch to handle one message at a time
    await this.channel.prefetch(1);
    
    // Consume acknowledgment messages
    await this.channel.consume(queues.ack, (msg) => {
      if (msg) {
        this.handleAcknowledgment(msg);
      }
    }, { noAck: false });
    
    logger.info('Consumer setup for acknowledgment queue');
  }

  handleAcknowledgment(msg) {
    try {
      const content = JSON.parse(msg.content.toString());
      const correlationId = msg.properties.correlationId;
      
      logger.info(`Received acknowledgment for correlation ID: ${correlationId}`, content);
      
      // Find and execute the response handler
      const handler = this.responseHandlers.get(correlationId);
      if (handler) {
        handler.resolve(content);
        this.responseHandlers.delete(correlationId);
      } else {
        logger.warn(`No handler found for correlation ID: ${correlationId}`);
      }
      
      // Acknowledge the message
      this.channel.ack(msg);
    } catch (error) {
      logger.error('Error handling acknowledgment:', error);
      // Reject the message without requeue
      this.channel.nack(msg, false, false);
    }
  }

  async publishRegistration(registrationData) {
    const { exchange, routingKeys } = config.rabbitmq;
    const correlationId = uuidv4();
    
    try {
      // Create a promise that will be resolved when we receive the response
      const responsePromise = new Promise((resolve, reject) => {
        // Set timeout for response
        const timeout = setTimeout(() => {
          this.responseHandlers.delete(correlationId);
          reject(new Error('Response timeout'));
        }, config.server.requestTimeout);
        
        // Store the handler
        this.responseHandlers.set(correlationId, {
          resolve: (data) => {
            clearTimeout(timeout);
            resolve(data);
          },
          reject: (error) => {
            clearTimeout(timeout);
            reject(error);
          },
        });
      });
      
      // Publish the message
      const message = Buffer.from(JSON.stringify(registrationData));
      const options = {
        persistent: true,
        correlationId: correlationId,
        replyTo: config.rabbitmq.queues.ack,
        timestamp: Date.now(),
      };
      
      const published = this.channel.publish(
        exchange,
        routingKeys.signup,
        message,
        options
      );
      
      if (!published) {
        throw new Error('Failed to publish message');
      }
      
      logger.info(`Published registration request with correlation ID: ${correlationId}`);
      
      // Wait for response
      return await responsePromise;
      
    } catch (error) {
      logger.error('Error publishing registration:', error);
      // Clean up handler if still exists
      this.responseHandlers.delete(correlationId);
      throw error;
    }
  }

  async close() {
    try {
      if (this.channel) {
        await this.channel.close();
      }
      if (this.connection) {
        await this.connection.close();
      }
      logger.info('RabbitMQ connection closed');
    } catch (error) {
      logger.error('Error closing RabbitMQ connection:', error);
    }
  }

  isConnected() {
    return this.connection && this.channel && !this.connection.closed;
  }
}

// Export singleton instance
export const rabbitmqService = new RabbitMQService();
export default rabbitmqService; 
"""RabbitMQ configuration and connection setup."""

import pika
import logging
from typing import Optional
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.retry import with_retry, RetryOptions, is_retryable_error

logger = logging.getLogger(__name__)


class RabbitMQConnection:
    """Manages RabbitMQ connection and channel."""

    def __init__(self, config):
        """Initialize RabbitMQ connection parameters."""
        self.config = config
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.channel.Channel] = None
        self.is_reconnecting = False

    @with_retry(
        options=RetryOptions(max_retries=5, initial_delay=2.0),
        context="RabbitMQ connection",
    )
    def connect(self):
        """Establish connection to RabbitMQ with retry logic."""
        try:
            credentials = pika.PlainCredentials(
                self.config.RABBITMQ_USERNAME, self.config.RABBITMQ_PASSWORD
            )

            parameters = pika.ConnectionParameters(
                host=self.config.RABBITMQ_HOST,
                port=self.config.RABBITMQ_PORT,
                virtual_host=self.config.RABBITMQ_VHOST,
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300,
            )

            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()

            # Declare exchange
            self.channel.exchange_declare(
                exchange=self.config.RABBITMQ_EXCHANGE,
                exchange_type="direct",
                durable=True,
            )

            # Declare queue
            self.channel.queue_declare(
                queue=self.config.RABBITMQ_QUEUE_VALIDATE, durable=True
            )

            # Bind queue to exchange
            self.channel.queue_bind(
                exchange=self.config.RABBITMQ_EXCHANGE,
                queue=self.config.RABBITMQ_QUEUE_VALIDATE,
                routing_key=self.config.RABBITMQ_ROUTING_KEY_VALIDATE,
            )

            logger.info("Successfully connected to RabbitMQ")

        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            raise

    def disconnect(self):
        """Close RabbitMQ connection."""
        if self.connection and not self.connection.is_closed:
            self.connection.close()
            logger.info("Disconnected from RabbitMQ")

    def is_connected(self) -> bool:
        """Check if connection is active."""
        return (
            self.connection is not None
            and not self.connection.is_closed
            and self.channel is not None
            and self.channel.is_open
        )

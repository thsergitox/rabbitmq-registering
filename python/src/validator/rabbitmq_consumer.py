"""RabbitMQ consumer for validation requests."""

import json
import logging
import pika
from typing import Optional
from config.rabbitmq import RabbitMQConnection
from validator.dto import ValidationRequest, ValidationResponse
from validator.validation_service import ValidationService
from validator.rabbitmq_publisher import ValidationPublisher

logger = logging.getLogger(__name__)


class ValidationConsumer:
    """Consumer for validation requests from RabbitMQ."""

    def __init__(self, rabbitmq_connection: RabbitMQConnection, db_connection=None):
        """Initialize the consumer.

        Args:
            rabbitmq_connection: RabbitMQ connection instance
            db_connection: Database connection for validation
        """
        self.rabbitmq_connection = rabbitmq_connection
        self.db_connection = db_connection
        self.channel = None

        # Initialize validation service and publisher
        self.validation_service = (
            ValidationService(db_connection) if db_connection else None
        )
        self.publisher = ValidationPublisher(rabbitmq_connection)

    def start_consuming(self):
        """Start consuming messages from the queue."""
        try:
            if not self.rabbitmq_connection.is_connected():
                logger.error("RabbitMQ connection is not active")
                return

            self.channel = self.rabbitmq_connection.channel

            # Set QoS
            self.channel.basic_qos(prefetch_count=1)

            # Start consuming
            self.channel.basic_consume(
                queue=self.rabbitmq_connection.config.RABBITMQ_QUEUE_VALIDATE,
                on_message_callback=self._on_message,
                auto_ack=False,
            )

            logger.info(
                "Started consuming from queue: %s",
                self.rabbitmq_connection.config.RABBITMQ_QUEUE_VALIDATE,
            )

            # Start consuming (blocking)
            self.channel.start_consuming()

        except Exception as e:
            logger.error(f"Error in consumer: {e}")
            raise

    def stop_consuming(self):
        """Stop consuming messages."""
        if self.channel and self.channel.is_open:
            try:
                self.channel.stop_consuming()
                logger.info("Stopped consuming messages")
            except Exception as e:
                logger.error(f"Error stopping consumer: {e}")

    def _on_message(self, channel, method, properties, body):
        """Handle incoming message.

        Args:
            channel: Channel instance
            method: Method frame with routing info
            properties: Message properties
            body: Message body (bytes)
        """
        try:
            # Decode message
            message_str = body.decode("utf-8")
            logger.info(f"Received message: {message_str}")

            # Parse validation request
            validation_request = self._parse_request(message_str)

            if validation_request:
                logger.info(f"Processing validation for DNI: {validation_request.dni}")

                # Task 12: Perform validation using validation service
                if self.validation_service:
                    validation_response = self.validation_service.validate_registration(
                        validation_request
                    )
                    logger.info(
                        f"Validation result for DNI {validation_request.dni}: "
                        f"{validation_response.status} - {validation_response.message}"
                    )

                    # Task 13: Publish response back via publisher
                    success = self.publisher.publish_validation_response(
                        validation_response
                    )

                    if success:
                        logger.info(f"Successfully published validation response")
                        # Acknowledge message only if we successfully published response
                        channel.basic_ack(delivery_tag=method.delivery_tag)
                        logger.info("Message acknowledged")
                    else:
                        logger.error("Failed to publish validation response")
                        # Reject and requeue if we couldn't publish response
                        channel.basic_nack(
                            delivery_tag=method.delivery_tag, requeue=True
                        )
                else:
                    logger.error("Validation service not initialized")
                    # Reject message and requeue
                    channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            else:
                logger.error("Failed to parse validation request")
                # Reject message and requeue
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

        except Exception as e:
            logger.error(f"Error processing message: {e}")
            # Reject message and requeue
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def _parse_request(self, message_str: str) -> Optional[ValidationRequest]:
        """Parse validation request from message.

        Args:
            message_str: JSON message string

        Returns:
            ValidationRequest if valid, None otherwise
        """
        try:
            request = ValidationRequest.from_json(message_str)

            # Validate required fields
            if not all(
                [
                    request.nombre,
                    request.correo,
                    request.dni,
                    request.telefono,
                    request.clave is not None,
                ]
            ):
                logger.error("Invalid request: missing required fields")
                return None

            return request

        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}")
            return None
        except Exception as e:
            logger.error(f"Failed to create ValidationRequest: {e}")
            return None

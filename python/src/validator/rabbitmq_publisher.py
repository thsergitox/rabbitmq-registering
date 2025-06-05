"""RabbitMQ publisher for validation responses."""

import json
import logging
import pika
from config.rabbitmq import RabbitMQConnection
from validator.dto import ValidationResponse

logger = logging.getLogger(__name__)


class ValidationPublisher:
    """Publisher for validation responses to RabbitMQ."""

    def __init__(self, rabbitmq_connection: RabbitMQConnection):
        """Initialize the publisher.

        Args:
            rabbitmq_connection: RabbitMQ connection instance
        """
        self.rabbitmq_connection = rabbitmq_connection
        self.config = rabbitmq_connection.config

    def publish_validation_response(self, response: ValidationResponse) -> bool:
        """Publish validation response to RabbitMQ.

        Args:
            response: ValidationResponse to publish

        Returns:
            True if published successfully, False otherwise
        """
        try:
            if not self.rabbitmq_connection.is_connected():
                logger.error("RabbitMQ connection is not active")
                return False

            channel = self.rabbitmq_connection.channel

            # Determine routing key based on status
            routing_key = (
                self.config.RABBITMQ_ROUTING_KEY_OK
                if response.status == "OK"
                else self.config.RABBITMQ_ROUTING_KEY_FAIL
            )

            # Convert response to JSON
            message_body = response.to_json()

            # Publish message
            channel.basic_publish(
                exchange=self.config.RABBITMQ_EXCHANGE,
                routing_key=routing_key,
                body=message_body,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                    content_type="application/json",
                ),
            )

            logger.info(
                f"Published validation response for DNI {response.dni} "
                f"with status {response.status} to routing key {routing_key}"
            )

            return True

        except Exception as e:
            logger.error(f"Failed to publish validation response: {e}")
            return False

    def publish_error_response(self, dni: int, error_message: str) -> bool:
        """Publish an error response for a failed validation.

        Args:
            dni: The DNI that failed validation
            error_message: Error message to send

        Returns:
            True if published successfully, False otherwise
        """
        response = ValidationResponse(
            dni=dni, status="FAIL", message=f"Validation error: {error_message}"
        )
        return self.publish_validation_response(response)

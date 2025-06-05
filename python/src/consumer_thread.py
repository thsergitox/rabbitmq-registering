"""Thread wrapper for RabbitMQ consumer."""

import threading
import logging
from validator.rabbitmq_consumer import ValidationConsumer

logger = logging.getLogger(__name__)


class ConsumerThread(threading.Thread):
    """Thread to run RabbitMQ consumer."""

    def __init__(self, rabbitmq_connection, db_connection=None):
        """Initialize consumer thread.

        Args:
            rabbitmq_connection: RabbitMQ connection instance
            db_connection: Database connection instance
        """
        super().__init__(daemon=True)
        self.rabbitmq_connection = rabbitmq_connection
        self.db_connection = db_connection
        self.consumer = ValidationConsumer(rabbitmq_connection, db_connection)
        self._stop_event = threading.Event()

    def run(self):
        """Run the consumer in the thread."""
        try:
            logger.info("Starting consumer thread")
            if self.rabbitmq_connection.is_connected():
                self.consumer.start_consuming()
            else:
                logger.error("Cannot start consumer: RabbitMQ not connected")
        except Exception as e:
            logger.error(f"Consumer thread error: {e}")

    def stop(self):
        """Stop the consumer thread."""
        logger.info("Stopping consumer thread")
        self._stop_event.set()
        try:
            self.consumer.stop_consuming()
        except Exception as e:
            logger.error(f"Error stopping consumer: {e}")

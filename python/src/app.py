"""Main Flask application."""

import os
import logging
import atexit
from flask import Flask, jsonify
from flask_cors import CORS
from config.config import config
from sqlalchemy import text
from config.database import DatabaseConnection
from config.rabbitmq import RabbitMQConnection
from consumer_thread import ConsumerThread
from logging.config import dictConfig

# Configure logging
dictConfig(
    {
        "version": 1,
        "formatters": {
            "default": {
                "format": "[%(asctime)s] %(levelname)s in %(module)s: %(message)s",
            }
        },
        "handlers": {
            "wsgi": {
                "class": "logging.StreamHandler",
                "stream": "ext://flask.logging.wsgi_errors_stream",
                "formatter": "default",
            }
        },
        "root": {"level": "INFO", "handlers": ["wsgi"]},
    }
)

logger = logging.getLogger(__name__)

# Global connections and consumer thread
db_connection = None
rabbitmq_connection = None
consumer_thread = None


def cleanup_consumer():
    """Cleanup consumer thread on shutdown."""
    global consumer_thread
    if consumer_thread and consumer_thread.is_alive():
        logger.info("Stopping consumer thread")
        consumer_thread.stop()
        consumer_thread.join(timeout=5)
        logger.info("Consumer thread stopped")


def create_app(config_name=None):
    """Create and configure Flask application."""
    if config_name is None:
        config_name = os.getenv("FLASK_ENV", "development")

    app = Flask(__name__)
    app.config.from_object(config[config_name])

    # Enable CORS
    CORS(app)

    # Initialize connections
    global db_connection, rabbitmq_connection

    db_connection = DatabaseConnection(app.config)
    rabbitmq_connection = RabbitMQConnection(app.config)

    # Connect to services
    try:
        db_connection.connect()
        rabbitmq_connection.connect()

        # Start consumer thread if RabbitMQ is connected
        global consumer_thread
        if rabbitmq_connection.is_connected():
            consumer_thread = ConsumerThread(rabbitmq_connection, db_connection)
            consumer_thread.start()
            logger.info("Started RabbitMQ consumer thread")

            # Register cleanup
            atexit.register(cleanup_consumer)

    except Exception as e:
        logger.error(f"Failed to initialize connections: {e}")
        # Don't fail app startup, connections can be retried

    # Register routes
    @app.route("/")
    def index():
        """Root endpoint."""
        return jsonify(
            {
                "service": "Validator Service (LP2)",
                "version": "1.0.0",
                "status": "running",
            }
        )

    @app.route("/api/health")
    def health():
        """Health check endpoint."""
        health_status = {
            "service": "Validator Service (LP2)",
            "status": "healthy",
            "checks": {
                "database": "unknown",
                "rabbitmq": "unknown",
                "consumer": "unknown",
            },
        }

        # Check database connection
        try:
            if db_connection and db_connection.engine:
                with db_connection.engine.connect() as conn:
                    conn.execute(text("SELECT 1"))
                health_status["checks"]["database"] = "healthy"
        except Exception as e:
            health_status["checks"]["database"] = f"unhealthy: {str(e)}"
            health_status["status"] = "degraded"

        # Check RabbitMQ connection
        try:
            if rabbitmq_connection and rabbitmq_connection.is_connected():
                health_status["checks"]["rabbitmq"] = "healthy"
            else:
                health_status["checks"]["rabbitmq"] = "unhealthy: not connected"
                health_status["status"] = "degraded"
        except Exception as e:
            health_status["checks"]["rabbitmq"] = f"unhealthy: {str(e)}"
            health_status["status"] = "degraded"

        # Check consumer thread
        try:
            if consumer_thread and consumer_thread.is_alive():
                health_status["checks"]["consumer"] = "healthy"
            else:
                health_status["checks"]["consumer"] = "unhealthy: not running"
                if health_status["checks"]["rabbitmq"] == "healthy":
                    health_status["status"] = "degraded"
        except Exception as e:
            health_status["checks"]["consumer"] = f"unhealthy: {str(e)}"
            health_status["status"] = "degraded"

        status_code = 200 if health_status["status"] == "healthy" else 503
        return jsonify(health_status), status_code

    # Cleanup on shutdown
    @app.teardown_appcontext
    def shutdown_connections(error=None):
        """Close connections on app context teardown."""
        if error:
            logger.error(f"Application error: {error}")

    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host=app.config["HOST"], port=app.config["PORT"], debug=app.config["DEBUG"])

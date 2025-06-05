"""Unit tests for RabbitMQ publisher."""

import json
import pytest
from unittest.mock import Mock, MagicMock, patch
import pika
from validator.rabbitmq_publisher import ValidationPublisher
from validator.dto import ValidationResponse


class TestValidationPublisher:
    """Test cases for ValidationPublisher."""

    @pytest.fixture
    def mock_rabbitmq_connection(self):
        """Create mock RabbitMQ connection."""
        mock_conn = Mock()
        mock_channel = Mock()
        mock_config = Mock()

        # Configure mock config
        mock_config.RABBITMQ_EXCHANGE = "registro_bus"
        mock_config.RABBITMQ_ROUTING_KEY_OK = "lp2.query.ok"
        mock_config.RABBITMQ_ROUTING_KEY_FAIL = "lp2.query.fail"

        # Configure mock connection
        mock_conn.config = mock_config
        mock_conn.channel = mock_channel
        mock_conn.is_connected.return_value = True

        return mock_conn, mock_channel, mock_config

    @pytest.fixture
    def publisher(self, mock_rabbitmq_connection):
        """Create publisher with mock dependencies."""
        mock_conn, _, _ = mock_rabbitmq_connection
        return ValidationPublisher(mock_conn)

    def test_publish_success_response(self, publisher, mock_rabbitmq_connection):
        """Test publishing a successful validation response."""
        _, mock_channel, mock_config = mock_rabbitmq_connection

        response = ValidationResponse(
            dni=12345678,
            status="OK",
            message="Validation successful",
        )

        result = publisher.publish_validation_response(response)

        assert result is True
        mock_channel.basic_publish.assert_called_once()

        # Verify call arguments
        call_args = mock_channel.basic_publish.call_args
        assert call_args[1]["exchange"] == "registro_bus"
        assert call_args[1]["routing_key"] == "lp2.query.ok"
        assert call_args[1]["body"] == response.to_json()

        # Verify message properties
        properties = call_args[1]["properties"]
        assert properties.delivery_mode == 2  # Persistent
        assert properties.content_type == "application/json"

    def test_publish_failure_response(self, publisher, mock_rabbitmq_connection):
        """Test publishing a failed validation response."""
        _, mock_channel, mock_config = mock_rabbitmq_connection

        response = ValidationResponse(
            dni=99999999,
            status="FAIL",
            message="DNI not found",
            invalidFriends=[11111111, 22222222],
        )

        result = publisher.publish_validation_response(response)

        assert result is True
        mock_channel.basic_publish.assert_called_once()

        # Verify routing key for failure
        call_args = mock_channel.basic_publish.call_args
        assert call_args[1]["routing_key"] == "lp2.query.fail"

        # Verify message body includes invalid friends
        body = json.loads(call_args[1]["body"])
        assert body["invalidFriends"] == [11111111, 22222222]

    def test_publish_when_not_connected(self, publisher, mock_rabbitmq_connection):
        """Test publishing when RabbitMQ is not connected."""
        mock_conn, _, _ = mock_rabbitmq_connection
        mock_conn.is_connected.return_value = False

        response = ValidationResponse(
            dni=12345678,
            status="OK",
            message="Test",
        )

        result = publisher.publish_validation_response(response)

        assert result is False

    def test_publish_handles_exception(self, publisher, mock_rabbitmq_connection):
        """Test publishing handles exceptions gracefully."""
        _, mock_channel, _ = mock_rabbitmq_connection
        mock_channel.basic_publish.side_effect = Exception("Connection lost")

        response = ValidationResponse(
            dni=12345678,
            status="OK",
            message="Test",
        )

        result = publisher.publish_validation_response(response)

        assert result is False

    def test_publish_error_response(self, publisher, mock_rabbitmq_connection):
        """Test publishing an error response using helper method."""
        _, mock_channel, _ = mock_rabbitmq_connection

        result = publisher.publish_error_response(
            dni=12345678,
            error_message="Database connection failed",
        )

        assert result is True
        mock_channel.basic_publish.assert_called_once()

        # Verify message content
        call_args = mock_channel.basic_publish.call_args
        body = json.loads(call_args[1]["body"])
        assert body["dni"] == 12345678
        assert body["status"] == "FAIL"
        assert "Database connection failed" in body["message"]

    def test_publish_with_pika_properties(self, publisher, mock_rabbitmq_connection):
        """Test that pika.BasicProperties is used correctly."""
        _, mock_channel, _ = mock_rabbitmq_connection

        response = ValidationResponse(
            dni=12345678,
            status="OK",
            message="Test",
        )

        with patch(
            "validator.rabbitmq_publisher.pika.BasicProperties"
        ) as mock_properties:
            mock_props_instance = Mock()
            mock_properties.return_value = mock_props_instance

            result = publisher.publish_validation_response(response)

            assert result is True
            mock_properties.assert_called_once_with(
                delivery_mode=2,
                content_type="application/json",
            )

            # Verify properties were passed to publish
            call_args = mock_channel.basic_publish.call_args
            assert call_args[1]["properties"] == mock_props_instance

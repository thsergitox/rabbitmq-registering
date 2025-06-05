"""Unit tests for RabbitMQ consumer."""

import sys
import os
import json
from unittest.mock import Mock, MagicMock, patch

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from validator.rabbitmq_consumer import ValidationConsumer
from validator.dto import ValidationRequest


def test_consumer_initialization():
    """Test consumer initialization."""
    mock_rabbitmq = Mock()
    mock_db = Mock()

    consumer = ValidationConsumer(mock_rabbitmq, mock_db)

    assert consumer.rabbitmq_connection == mock_rabbitmq
    assert consumer.db_connection == mock_db
    assert consumer.channel is None


def test_parse_request_valid():
    """Test parsing a valid validation request."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    valid_json = json.dumps(
        {
            "nombre": "Test User",
            "correo": "test@example.com",
            "clave": 12345,
            "dni": 12345678,
            "telefono": 987654321,
            "friendsDni": [11111111, 22222222],
        }
    )

    request = consumer._parse_request(valid_json)

    assert request is not None
    assert isinstance(request, ValidationRequest)
    assert request.nombre == "Test User"
    assert request.dni == 12345678
    assert request.friendsDni == [11111111, 22222222]


def test_parse_request_invalid_json():
    """Test parsing invalid JSON."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    invalid_json = "not a json string"

    request = consumer._parse_request(invalid_json)

    assert request is None


def test_parse_request_missing_fields():
    """Test parsing request with missing required fields."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    # Missing 'nombre' field
    incomplete_json = json.dumps(
        {
            "correo": "test@example.com",
            "clave": 12345,
            "dni": 12345678,
            "telefono": 987654321,
        }
    )

    request = consumer._parse_request(incomplete_json)

    assert request is None


def test_on_message_valid():
    """Test processing a valid message."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    # Mock channel and method
    mock_channel = Mock()
    mock_method = Mock()
    mock_method.delivery_tag = 123
    mock_properties = Mock()

    valid_message = json.dumps(
        {
            "nombre": "Test User",
            "correo": "test@example.com",
            "clave": 12345,
            "dni": 12345678,
            "telefono": 987654321,
            "friendsDni": [],
        }
    )

    # Call the message handler
    consumer._on_message(
        mock_channel, mock_method, mock_properties, valid_message.encode()
    )

    # Verify the message was acknowledged
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=123)


def test_on_message_invalid():
    """Test processing an invalid message."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    # Mock channel and method
    mock_channel = Mock()
    mock_method = Mock()
    mock_method.delivery_tag = 456
    mock_properties = Mock()

    invalid_message = b"invalid json"

    # Call the message handler
    consumer._on_message(mock_channel, mock_method, mock_properties, invalid_message)

    # Verify the message was rejected and requeued
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=456, requeue=True)


def test_start_consuming_not_connected():
    """Test start consuming when RabbitMQ is not connected."""
    mock_rabbitmq = Mock()
    mock_rabbitmq.is_connected.return_value = False

    consumer = ValidationConsumer(mock_rabbitmq)

    # Should return without error when not connected
    consumer.start_consuming()

    # Channel should not be created
    assert consumer.channel is None


def test_stop_consuming():
    """Test stopping the consumer."""
    mock_rabbitmq = Mock()
    consumer = ValidationConsumer(mock_rabbitmq)

    # Mock channel
    mock_channel = Mock()
    mock_channel.is_open = True
    consumer.channel = mock_channel

    consumer.stop_consuming()

    mock_channel.stop_consuming.assert_called_once()


if __name__ == "__main__":
    print("Testing RabbitMQ consumer...\n")

    test_consumer_initialization()
    print("✅ Consumer initialization test passed")

    test_parse_request_valid()
    print("✅ Parse valid request test passed")

    test_parse_request_invalid_json()
    print("✅ Parse invalid JSON test passed")

    test_parse_request_missing_fields()
    print("✅ Parse missing fields test passed")

    test_on_message_valid()
    print("✅ Process valid message test passed")

    test_on_message_invalid()
    print("✅ Process invalid message test passed")

    test_start_consuming_not_connected()
    print("✅ Start consuming not connected test passed")

    test_stop_consuming()
    print("✅ Stop consuming test passed")

    print("\n✅ All consumer tests passed!")

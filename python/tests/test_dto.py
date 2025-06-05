"""Unit tests for validation DTOs."""

import sys
import os
import json
from datetime import datetime

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from validator.dto import ValidationRequest, ValidationResponse


def test_validation_request_creation():
    """Test creating a ValidationRequest."""
    request = ValidationRequest(
        nombre="John Doe",
        correo="john@example.com",
        clave=12345,
        dni=12345678,
        telefono=987654321,
        friendsDni=[11111111, 22222222],
    )

    assert request.nombre == "John Doe"
    assert request.correo == "john@example.com"
    assert request.clave == 12345
    assert request.dni == 12345678
    assert request.telefono == 987654321
    assert request.friendsDni == [11111111, 22222222]


def test_validation_request_from_json():
    """Test creating ValidationRequest from JSON."""
    json_str = json.dumps(
        {
            "nombre": "Jane Smith",
            "correo": "jane@example.com",
            "clave": 54321,
            "dni": 87654321,
            "telefono": 123456789,
            "friendsDni": [33333333, 44444444, 55555555],
        }
    )

    request = ValidationRequest.from_json(json_str)

    assert request.nombre == "Jane Smith"
    assert request.correo == "jane@example.com"
    assert request.clave == 54321
    assert request.dni == 87654321
    assert request.telefono == 123456789
    assert request.friendsDni == [33333333, 44444444, 55555555]


def test_validation_request_from_json_without_friends():
    """Test creating ValidationRequest from JSON without friendsDni."""
    json_str = json.dumps(
        {
            "nombre": "Bob Johnson",
            "correo": "bob@example.com",
            "clave": 99999,
            "dni": 11223344,
            "telefono": 555666777,
        }
    )

    request = ValidationRequest.from_json(json_str)

    assert request.nombre == "Bob Johnson"
    assert request.correo == "bob@example.com"
    assert request.friendsDni == []  # Default empty list


def test_validation_request_to_dict():
    """Test converting ValidationRequest to dictionary."""
    request = ValidationRequest(
        nombre="Alice Brown",
        correo="alice@example.com",
        clave=11111,
        dni=99887766,
        telefono=444333222,
        friendsDni=[66666666],
    )

    result = request.to_dict()

    assert result == {
        "nombre": "Alice Brown",
        "correo": "alice@example.com",
        "clave": 11111,
        "dni": 99887766,
        "telefono": 444333222,
        "friendsDni": [66666666],
    }


def test_validation_response_creation():
    """Test creating a ValidationResponse."""
    response = ValidationResponse(
        dni=12345678,
        status="OK",
        message="Validation successful",
        invalidFriends=[11111111, 22222222],
    )

    assert response.dni == 12345678
    assert response.status == "OK"
    assert response.message == "Validation successful"
    assert response.invalidFriends == [11111111, 22222222]
    assert response.timestamp  # Should have a timestamp


def test_validation_response_to_json():
    """Test converting ValidationResponse to JSON."""
    response = ValidationResponse(
        dni=87654321, status="FAIL", message="DNI not found", invalidFriends=[]
    )

    json_str = response.to_json()
    data = json.loads(json_str)

    assert data["dni"] == 87654321
    assert data["status"] == "FAIL"
    assert data["message"] == "DNI not found"
    assert data["invalidFriends"] == []
    assert "timestamp" in data


def test_validation_response_to_dict():
    """Test converting ValidationResponse to dictionary."""
    response = ValidationResponse(
        dni=55443322,
        status="OK",
        message="All validations passed",
        timestamp="2025-01-01T12:00:00",
        invalidFriends=[99999999],
    )

    result = response.to_dict()

    assert result == {
        "dni": 55443322,
        "status": "OK",
        "message": "All validations passed",
        "timestamp": "2025-01-01T12:00:00",
        "invalidFriends": [99999999],
    }


if __name__ == "__main__":
    print("Testing validation DTOs...\n")

    test_validation_request_creation()
    print("✅ ValidationRequest creation test passed")

    test_validation_request_from_json()
    print("✅ ValidationRequest from JSON test passed")

    test_validation_request_from_json_without_friends()
    print("✅ ValidationRequest from JSON without friends test passed")

    test_validation_request_to_dict()
    print("✅ ValidationRequest to_dict test passed")

    test_validation_response_creation()
    print("✅ ValidationResponse creation test passed")

    test_validation_response_to_json()
    print("✅ ValidationResponse to JSON test passed")

    test_validation_response_to_dict()
    print("✅ ValidationResponse to_dict test passed")

    print("\n✅ All DTO tests passed!")

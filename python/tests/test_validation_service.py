"""Unit tests for validation service."""

import pytest
from unittest.mock import Mock, MagicMock, patch
from validator.validation_service import ValidationService
from validator.dto import ValidationRequest, ValidationResponse
from models.persona_repository import PersonaRepository


class TestValidationService:
    """Test cases for ValidationService."""

    @pytest.fixture
    def mock_db_connection(self):
        """Create mock database connection."""
        mock_conn = Mock()
        mock_session = Mock()
        mock_conn.get_session.return_value = mock_session
        return mock_conn, mock_session

    @pytest.fixture
    def validation_service(self, mock_db_connection):
        """Create validation service with mock dependencies."""
        mock_conn, _ = mock_db_connection
        return ValidationService(mock_conn)

    def test_validate_dni_exists_no_friends(
        self, validation_service, mock_db_connection
    ):
        """Test validation when DNI exists and no friends are specified."""
        _, mock_session = mock_db_connection
        mock_repo = Mock(spec=PersonaRepository)
        mock_repo.exists_by_dni.return_value = True

        # Mock PersonaRepository constructor
        with patch(
            "validator.validation_service.PersonaRepository", return_value=mock_repo
        ):
            request = ValidationRequest(
                nombre="Test User",
                correo="test@example.com",
                clave=123456,
                dni=12345678,
                telefono=987654321,
                friendsDni=[],
            )

            response = validation_service.validate_registration(request)

            assert response.dni == 12345678
            assert response.status == "OK"
            assert response.message == "DNI and all friends validated successfully"
            assert response.invalidFriends == []
            mock_repo.exists_by_dni.assert_called_once_with(12345678)
            mock_session.close.assert_called_once()

    def test_validate_dni_not_exists(self, validation_service, mock_db_connection):
        """Test validation when DNI does not exist."""
        _, mock_session = mock_db_connection
        mock_repo = Mock(spec=PersonaRepository)
        mock_repo.exists_by_dni.return_value = False

        with patch(
            "validator.validation_service.PersonaRepository", return_value=mock_repo
        ):
            request = ValidationRequest(
                nombre="Test User",
                correo="test@example.com",
                clave=123456,
                dni=99999999,
                telefono=987654321,
                friendsDni=[],
            )

            response = validation_service.validate_registration(request)

            assert response.dni == 99999999
            assert response.status == "FAIL"
            assert "99999999 is not registered" in response.message
            assert response.invalidFriends == []

    def test_validate_with_all_valid_friends(
        self, validation_service, mock_db_connection
    ):
        """Test validation when all friends exist."""
        _, mock_session = mock_db_connection
        mock_repo = Mock(spec=PersonaRepository)
        mock_repo.exists_by_dni.return_value = True
        mock_repo.check_multiple_exist.return_value = {
            11111111: True,
            22222222: True,
            33333333: True,
        }

        with patch(
            "validator.validation_service.PersonaRepository", return_value=mock_repo
        ):
            request = ValidationRequest(
                nombre="Test User",
                correo="test@example.com",
                clave=123456,
                dni=12345678,
                telefono=987654321,
                friendsDni=[11111111, 22222222, 33333333],
            )

            response = validation_service.validate_registration(request)

            assert response.status == "OK"
            assert response.invalidFriends == []
            mock_repo.check_multiple_exist.assert_called_once_with(
                [11111111, 22222222, 33333333]
            )

    def test_validate_with_some_invalid_friends(
        self, validation_service, mock_db_connection
    ):
        """Test validation when some friends don't exist."""
        _, mock_session = mock_db_connection
        mock_repo = Mock(spec=PersonaRepository)
        mock_repo.exists_by_dni.return_value = True
        mock_repo.check_multiple_exist.return_value = {
            11111111: True,
            22222222: False,
            33333333: False,
        }

        with patch(
            "validator.validation_service.PersonaRepository", return_value=mock_repo
        ):
            request = ValidationRequest(
                nombre="Test User",
                correo="test@example.com",
                clave=123456,
                dni=12345678,
                telefono=987654321,
                friendsDni=[11111111, 22222222, 33333333],
            )

            response = validation_service.validate_registration(request)

            assert response.status == "FAIL"
            assert "2 friends not found" in response.message
            assert sorted(response.invalidFriends) == [22222222, 33333333]

    def test_validate_handles_database_error(
        self, validation_service, mock_db_connection
    ):
        """Test validation handles database errors gracefully."""
        mock_conn, _ = mock_db_connection
        mock_conn.get_session.side_effect = Exception("Database connection failed")

        request = ValidationRequest(
            nombre="Test User",
            correo="test@example.com",
            clave=123456,
            dni=12345678,
            telefono=987654321,
            friendsDni=[],
        )

        response = validation_service.validate_registration(request)

        assert response.dni == 12345678
        assert response.status == "FAIL"
        assert "Database connection failed" in response.message

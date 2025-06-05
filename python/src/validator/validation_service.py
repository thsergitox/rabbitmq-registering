"""Validation service for DNI checking."""

import logging
from typing import List, Tuple
from models.persona_repository import PersonaRepository
from validator.dto import ValidationRequest, ValidationResponse

logger = logging.getLogger(__name__)


class ValidationService:
    """Service for validating DNI and friend DNIs."""

    def __init__(self, db_connection):
        """Initialize validation service with database connection."""
        self.db_connection = db_connection

    def validate_registration(self, request: ValidationRequest) -> ValidationResponse:
        """Validate a registration request.

        Args:
            request: ValidationRequest with user data and friend DNIs

        Returns:
            ValidationResponse with validation result
        """
        try:
            # Get database session
            session = self.db_connection.get_session()
            repository = PersonaRepository(session)

            try:
                # Check if the main DNI exists
                main_dni_exists = repository.exists_by_dni(request.dni)

                if not main_dni_exists:
                    logger.info(f"DNI {request.dni} not found in database")
                    return ValidationResponse(
                        dni=request.dni,
                        status="FAIL",
                        message=f"DNI {request.dni} is not registered in the system",
                    )

                # Check friend DNIs if any
                invalid_friends = []
                if request.friendsDni:
                    logger.info(f"Validating {len(request.friendsDni)} friend DNIs")
                    friends_existence = repository.check_multiple_exist(
                        request.friendsDni
                    )

                    # Find invalid friends
                    invalid_friends = [
                        dni for dni, exists in friends_existence.items() if not exists
                    ]

                    if invalid_friends:
                        logger.info(
                            f"Found {len(invalid_friends)} invalid friend DNIs: {invalid_friends}"
                        )
                        return ValidationResponse(
                            dni=request.dni,
                            status="FAIL",
                            message=f"Invalid friend DNIs: {len(invalid_friends)} friends not found",
                            invalidFriends=invalid_friends,
                        )

                # All validations passed
                logger.info(f"DNI {request.dni} and all friends validated successfully")
                return ValidationResponse(
                    dni=request.dni,
                    status="OK",
                    message="DNI and all friends validated successfully",
                )

            finally:
                session.close()

        except Exception as e:
            logger.error(f"Error during validation: {e}")
            return ValidationResponse(
                dni=request.dni,
                status="FAIL",
                message=f"Validation error: {str(e)}",
            )

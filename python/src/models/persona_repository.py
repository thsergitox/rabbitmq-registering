"""Repository for Persona data access."""

from typing import List, Optional
from sqlalchemy.orm import Session
from models.persona import Persona


class PersonaRepository:
    """Repository for Persona database operations."""

    def __init__(self, session: Session):
        """Initialize repository with database session."""
        self.session = session

    def find_by_dni(self, dni: int) -> Optional[Persona]:
        """Find a persona by DNI.

        Args:
            dni: The DNI to search for

        Returns:
            Persona instance if found, None otherwise
        """
        return self.session.query(Persona).filter_by(dni=dni).first()

    def find_multiple_by_dni(self, dnis: List[int]) -> List[Persona]:
        """Find multiple personas by a list of DNIs.

        Args:
            dnis: List of DNIs to search for

        Returns:
            List of Persona instances found
        """
        if not dnis:
            return []
        return self.session.query(Persona).filter(Persona.dni.in_(dnis)).all()

    def exists_by_dni(self, dni: int) -> bool:
        """Check if a persona exists by DNI.

        Args:
            dni: The DNI to check

        Returns:
            True if exists, False otherwise
        """
        return self.session.query(Persona).filter_by(dni=dni).count() > 0

    def check_multiple_exist(self, dnis: List[int]) -> dict:
        """Check which DNIs exist in the database.

        Args:
            dnis: List of DNIs to check

        Returns:
            Dictionary mapping DNI to existence (True/False)
        """
        if not dnis:
            return {}

        # Query all existing DNIs in one query
        existing_personas = self.find_multiple_by_dni(dnis)
        existing_dnis = {p.dni for p in existing_personas}

        # Create result dictionary
        return {dni: dni in existing_dnis for dni in dnis}

    def count(self) -> int:
        """Count total personas in the database.

        Returns:
            Total count of personas
        """
        return self.session.query(Persona).count()

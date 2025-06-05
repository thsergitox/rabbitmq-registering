"""Data Transfer Objects for validation messages."""

from typing import List, Optional
from dataclasses import dataclass, field
from datetime import datetime
import json


@dataclass
class ValidationRequest:
    """Represents a validation request from LP3."""

    nombre: str
    correo: str
    clave: int
    dni: int
    telefono: int
    friendsDni: List[int] = field(default_factory=list)

    @classmethod
    def from_json(cls, json_str: str) -> "ValidationRequest":
        """Create ValidationRequest from JSON string.

        Args:
            json_str: JSON string representation

        Returns:
            ValidationRequest instance
        """
        data = json.loads(json_str)
        return cls(
            nombre=data["nombre"],
            correo=data["correo"],
            clave=data["clave"],
            dni=data["dni"],
            telefono=data["telefono"],
            friendsDni=data.get("friendsDni", []),
        )

    def to_dict(self) -> dict:
        """Convert to dictionary."""
        return {
            "nombre": self.nombre,
            "correo": self.correo,
            "clave": self.clave,
            "dni": self.dni,
            "telefono": self.telefono,
            "friendsDni": self.friendsDni,
        }


@dataclass
class ValidationResponse:
    """Represents a validation response to be sent."""

    dni: int
    status: str  # 'OK' or 'FAIL'
    message: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    invalidFriends: List[int] = field(default_factory=list)

    def to_json(self) -> str:
        """Convert to JSON string.

        Returns:
            JSON string representation
        """
        return json.dumps(
            {
                "dni": self.dni,
                "status": self.status,
                "message": self.message,
                "timestamp": self.timestamp,
                "invalidFriends": self.invalidFriends,
            }
        )

    def to_dict(self) -> dict:
        """Convert to dictionary."""
        return {
            "dni": self.dni,
            "status": self.status,
            "message": self.message,
            "timestamp": self.timestamp,
            "invalidFriends": self.invalidFriends,
        }

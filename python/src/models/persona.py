"""Persona model for DNI validation."""

from sqlalchemy import Column, Integer, String
from config.database import Base


class Persona(Base):
    """Model representing a person in the DNI database."""

    __tablename__ = "persona"

    id = Column(Integer, primary_key=True)
    dni = Column(Integer, nullable=False, index=True)
    nombre = Column(String(512), nullable=False)
    apellidos = Column(String(512), nullable=False)
    lugar_nac = Column(String(512), nullable=False)
    ubigeo = Column(Integer, nullable=False)
    direccion = Column(String(512), nullable=False)

    def __repr__(self):
        """String representation of the Persona model."""
        return (
            f"<Persona(id={self.id}, dni={self.dni}, "
            f"nombre='{self.nombre}', apellidos='{self.apellidos}')>"
        )

    def to_dict(self):
        """Convert the model to a dictionary."""
        return {
            "id": self.id,
            "dni": self.dni,
            "nombre": self.nombre,
            "apellidos": self.apellidos,
            "lugar_nac": self.lugar_nac,
            "ubigeo": self.ubigeo,
            "direccion": self.direccion,
        }

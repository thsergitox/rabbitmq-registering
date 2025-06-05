"""Unit tests for the Persona model."""

import sys
import os
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from config.database import Base
from models.persona import Persona


@pytest.fixture
def db_session():
    """Create a test database session."""
    # Use SQLite in-memory database for testing
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)

    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    session = SessionLocal()

    yield session

    session.close()


def test_create_persona(db_session):
    """Test creating a new Persona instance."""
    persona = Persona(
        id=1,
        dni=12345678,
        nombre="Juan",
        apellidos="Pérez García",
        lugar_nac="Lima",
        ubigeo=150101,
        direccion="Av. Central 123",
    )

    db_session.add(persona)
    db_session.commit()

    # Query the persona back
    saved_persona = db_session.query(Persona).filter_by(dni=12345678).first()

    assert saved_persona is not None
    assert saved_persona.nombre == "Juan"
    assert saved_persona.apellidos == "Pérez García"
    assert saved_persona.lugar_nac == "Lima"
    assert saved_persona.ubigeo == 150101
    assert saved_persona.direccion == "Av. Central 123"


def test_persona_repr(db_session):
    """Test the string representation of Persona."""
    persona = Persona(
        id=2,
        dni=87654321,
        nombre="María",
        apellidos="González López",
        lugar_nac="Cusco",
        ubigeo=150102,
        direccion="Jr. Los Pinos 456",
    )

    expected_repr = (
        "<Persona(id=2, dni=87654321, nombre='María', apellidos='González López')>"
    )
    assert repr(persona) == expected_repr


def test_persona_to_dict(db_session):
    """Test converting Persona to dictionary."""
    persona = Persona(
        id=3,
        dni=11223344,
        nombre="Carlos",
        apellidos="Rodríguez Silva",
        lugar_nac="Arequipa",
        ubigeo=150103,
        direccion="Calle Las Flores 789",
    )

    persona_dict = persona.to_dict()

    assert persona_dict == {
        "id": 3,
        "dni": 11223344,
        "nombre": "Carlos",
        "apellidos": "Rodríguez Silva",
        "lugar_nac": "Arequipa",
        "ubigeo": 150103,
        "direccion": "Calle Las Flores 789",
    }


def test_query_by_dni(db_session):
    """Test querying personas by DNI."""
    # Create multiple personas
    personas = [
        Persona(
            id=i,
            dni=10000000 + i,
            nombre=f"Person {i}",
            apellidos=f"Apellido {i}",
            lugar_nac="Lima",
            ubigeo=150101,
            direccion=f"Direccion {i}",
        )
        for i in range(1, 6)
    ]

    db_session.add_all(personas)
    db_session.commit()

    # Query by specific DNI
    target_dni = 10000003
    result = db_session.query(Persona).filter_by(dni=target_dni).first()

    assert result is not None
    assert result.nombre == "Person 3"
    assert result.dni == target_dni


def test_query_multiple_dnis(db_session):
    """Test querying multiple personas by a list of DNIs."""
    # Create personas
    personas = [
        Persona(
            id=i,
            dni=20000000 + i,
            nombre=f"User {i}",
            apellidos=f"Lastname {i}",
            lugar_nac="Cusco",
            ubigeo=150102,
            direccion=f"Address {i}",
        )
        for i in range(1, 11)
    ]

    db_session.add_all(personas)
    db_session.commit()

    # Query by multiple DNIs
    target_dnis = [20000002, 20000005, 20000008]
    results = db_session.query(Persona).filter(Persona.dni.in_(target_dnis)).all()

    assert len(results) == 3
    found_dnis = [p.dni for p in results]
    assert set(found_dnis) == set(target_dnis)

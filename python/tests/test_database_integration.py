"""Integration tests for database connection and Persona model with MariaDB."""

import pytest
import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from sqlalchemy import text
from config.config import Config
from config.database import DatabaseConnection
from models.persona import Persona


def test_database_connection():
    """Test connecting to the MariaDB database."""
    config = Config()
    db_connection = DatabaseConnection(config)

    try:
        db_connection.connect()

        # Test the connection by executing a simple query
        with db_connection.engine.connect() as conn:
            result = conn.execute(text("SELECT 1"))
            assert result.fetchone()[0] == 1

        print("✅ Database connection successful")

    except Exception as e:
        pytest.fail(f"Failed to connect to database: {e}")
    finally:
        db_connection.disconnect()


def test_query_existing_personas():
    """Test querying existing personas from the database."""
    config = Config()
    db_connection = DatabaseConnection(config)

    try:
        db_connection.connect()
        session = db_connection.get_session()

        # Query first 5 personas
        personas = session.query(Persona).limit(5).all()

        assert len(personas) > 0
        print(f"✅ Found {len(personas)} personas")

        # Verify the structure of the first persona
        if personas:
            first_persona = personas[0]
            assert hasattr(first_persona, "dni")
            assert hasattr(first_persona, "nombre")
            assert hasattr(first_persona, "apellidos")
            assert hasattr(first_persona, "lugar_nac")
            assert hasattr(first_persona, "ubigeo")
            assert hasattr(first_persona, "direccion")

            print(f"✅ First persona: {first_persona}")

        session.close()

    except Exception as e:
        pytest.fail(f"Failed to query personas: {e}")
    finally:
        db_connection.disconnect()


def test_query_by_dni():
    """Test querying a specific persona by DNI."""
    config = Config()
    db_connection = DatabaseConnection(config)

    try:
        db_connection.connect()
        session = db_connection.get_session()

        # Use a known DNI from the sample data
        test_dni = 67741822  # Elena Pérez from the first record

        persona = session.query(Persona).filter_by(dni=test_dni).first()

        assert persona is not None
        assert persona.dni == test_dni
        assert persona.nombre == "Elena"
        assert persona.apellidos == "Pérez"

        print(f"✅ Found persona by DNI: {persona.to_dict()}")

        session.close()

    except Exception as e:
        pytest.fail(f"Failed to query by DNI: {e}")
    finally:
        db_connection.disconnect()


def test_query_multiple_dnis():
    """Test querying multiple personas by DNI list."""
    config = Config()
    db_connection = DatabaseConnection(config)

    try:
        db_connection.connect()
        session = db_connection.get_session()

        # Use known DNIs from the sample data
        test_dnis = [67741822, 54717606, 47323002]  # First 3 personas

        personas = session.query(Persona).filter(Persona.dni.in_(test_dnis)).all()

        assert len(personas) == 3
        found_dnis = {p.dni for p in personas}
        assert found_dnis == set(test_dnis)

        print(f"✅ Found {len(personas)} personas by DNI list")
        for persona in personas:
            print(f"  - DNI: {persona.dni}, Name: {persona.nombre} {persona.apellidos}")

        session.close()

    except Exception as e:
        pytest.fail(f"Failed to query multiple DNIs: {e}")
    finally:
        db_connection.disconnect()


def test_total_persona_count():
    """Test counting total personas in the database."""
    config = Config()
    db_connection = DatabaseConnection(config)

    try:
        db_connection.connect()
        session = db_connection.get_session()

        total_count = session.query(Persona).count()

        assert total_count > 0
        print(f"✅ Total personas in database: {total_count}")

        session.close()

    except Exception as e:
        pytest.fail(f"Failed to count personas: {e}")
    finally:
        db_connection.disconnect()


if __name__ == "__main__":
    # Run the tests manually for debugging
    print("Running database integration tests...\n")

    test_database_connection()
    print()

    test_query_existing_personas()
    print()

    test_query_by_dni()
    print()

    test_query_multiple_dnis()
    print()

    test_total_persona_count()
    print()

    print("\n✅ All integration tests passed!")

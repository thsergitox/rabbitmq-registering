"""Test the Flask application."""

import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from app import create_app


def test_app_creation():
    """Test Flask app creation."""
    app = create_app("testing")

    assert app is not None
    assert app.config["TESTING"] == True


def test_health_endpoint():
    """Test the health endpoint."""
    app = create_app("testing")
    client = app.test_client()

    response = client.get("/api/health")

    assert response.status_code in [200, 503]  # 503 if connections fail
    data = response.get_json()
    assert "service" in data
    assert data["service"] == "Validator Service (LP2)"
    assert "status" in data
    assert "checks" in data


def test_root_endpoint():
    """Test the root endpoint."""
    app = create_app("testing")
    client = app.test_client()

    response = client.get("/")

    assert response.status_code == 200
    data = response.get_json()
    assert data["service"] == "Validator Service (LP2)"
    assert data["version"] == "1.0.0"
    assert data["status"] == "running"


if __name__ == "__main__":
    print("Testing Flask app...")
    test_app_creation()
    print("✅ App creation test passed")

    test_health_endpoint()
    print("✅ Health endpoint test passed")

    test_root_endpoint()
    print("✅ Root endpoint test passed")

    print("\n✅ All Flask app tests passed!")

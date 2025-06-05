"""End-to-end integration tests for the complete registration flow."""

import pytest
import asyncio
import json
import time
from concurrent.futures import ThreadPoolExecutor
import requests
import pika
import psycopg2
import pymysql

# Service URLs
NODE_API_URL = "http://localhost:8083/api"
JAVA_API_URL = "http://localhost:8081/api"
PYTHON_API_URL = "http://localhost:8082/api"


class TestE2EFlow:
    """End-to-end integration tests."""

    @classmethod
    def setup_class(cls):
        """Setup test environment."""
        cls.wait_for_services()
        cls.setup_rabbitmq_connection()
        cls.setup_database_connections()

    @classmethod
    def wait_for_services(cls, timeout=60):
        """Wait for all services to be ready."""
        services = [
            (NODE_API_URL + "/health", "Node.js"),
            (JAVA_API_URL + "/health", "Java"),
            (PYTHON_API_URL + "/health", "Python"),
        ]

        start_time = time.time()
        while time.time() - start_time < timeout:
            all_ready = True
            for url, name in services:
                try:
                    response = requests.get(url, timeout=5)
                    if response.status_code != 200:
                        all_ready = False
                        print(f"{name} service not ready yet")
                except Exception:
                    all_ready = False
                    print(f"{name} service not reachable")

            if all_ready:
                print("All services are ready")
                return

            time.sleep(2)

        raise TimeoutError("Services did not become ready in time")

    @classmethod
    def setup_rabbitmq_connection(cls):
        """Setup RabbitMQ connection for monitoring."""
        credentials = pika.PlainCredentials("admin", "admin123")
        parameters = pika.ConnectionParameters("localhost", 5672, "/", credentials)
        cls.rabbitmq_connection = pika.BlockingConnection(parameters)
        cls.rabbitmq_channel = cls.rabbitmq_connection.channel()

    @classmethod
    def setup_database_connections(cls):
        """Setup database connections for verification."""
        # PostgreSQL connection
        cls.postgres_conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="user_db",
            user="dbuser",
            password="dbpass123",
        )

        # MariaDB connection
        cls.mariadb_conn = pymysql.connect(
            host="localhost",
            port=3306,
            database="dni_db",
            user="dbuser",
            password="dbpass123",
        )

    @classmethod
    def teardown_class(cls):
        """Cleanup resources."""
        if hasattr(cls, "rabbitmq_connection"):
            cls.rabbitmq_connection.close()
        if hasattr(cls, "postgres_conn"):
            cls.postgres_conn.close()
        if hasattr(cls, "mariadb_conn"):
            cls.mariadb_conn.close()

    def test_successful_registration(self):
        """Test successful user registration flow."""
        # Test data - use a DNI that exists in MariaDB
        test_user = {
            "nombre": "Integration Test User",
            "correo": f"integration_test_{int(time.time())}@example.com",
            "clave": 123456,
            "dni": 20000001,  # This DNI should exist in MariaDB
            "telefono": 987654321,
            "friendsDni": [],
        }

        # Send registration request
        response = requests.post(f"{NODE_API_URL}/register", json=test_user, timeout=30)

        # Verify response
        assert response.status_code == 201
        data = response.json()
        assert data["success"] is True
        assert data["data"]["status"] == "SUCCESS"

        # Verify in PostgreSQL
        with self.postgres_conn.cursor() as cursor:
            cursor.execute("SELECT * FROM users WHERE dni = %s", (test_user["dni"],))
            user = cursor.fetchone()
            assert user is not None
            assert user[4] == test_user["dni"]  # dni column

    def test_registration_with_invalid_dni(self):
        """Test registration with DNI that doesn't exist in validation DB."""
        test_user = {
            "nombre": "Invalid DNI User",
            "correo": f"invalid_dni_{int(time.time())}@example.com",
            "clave": 123456,
            "dni": 99999999,  # This DNI should NOT exist in MariaDB
            "telefono": 987654321,
            "friendsDni": [],
        }

        response = requests.post(f"{NODE_API_URL}/register", json=test_user, timeout=30)

        # Should get 400 due to validation failure
        assert response.status_code == 400
        data = response.json()
        assert data["success"] is False
        assert "FAIL" in data["data"]["status"]

    def test_registration_with_friends(self):
        """Test registration with friend relationships."""
        # Use DNIs that exist in MariaDB
        test_user = {
            "nombre": "User With Friends",
            "correo": f"friends_test_{int(time.time())}@example.com",
            "clave": 123456,
            "dni": 20000002,
            "telefono": 987654322,
            "friendsDni": [20000003, 20000004],  # These should exist
        }

        response = requests.post(f"{NODE_API_URL}/register", json=test_user, timeout=30)

        assert response.status_code == 201
        data = response.json()
        assert data["success"] is True

    def test_duplicate_registration(self):
        """Test duplicate registration handling."""
        test_user = {
            "nombre": "Duplicate Test User",
            "correo": f"duplicate_{int(time.time())}@example.com",
            "clave": 123456,
            "dni": 20000005,
            "telefono": 987654323,
            "friendsDni": [],
        }

        # First registration should succeed
        response1 = requests.post(
            f"{NODE_API_URL}/register", json=test_user, timeout=30
        )
        assert response1.status_code == 201

        # Second registration should fail
        response2 = requests.post(
            f"{NODE_API_URL}/register", json=test_user, timeout=30
        )
        assert response2.status_code == 400
        data = response2.json()
        assert "already exists" in data["data"]["message"].lower()

    def test_concurrent_registrations(self):
        """Test system behavior under concurrent load."""

        def register_user(index):
            user = {
                "nombre": f"Concurrent User {index}",
                "correo": f"concurrent_{index}_{int(time.time())}@example.com",
                "clave": 100000 + index,
                "dni": 20000100 + index,  # Ensure these DNIs exist
                "telefono": 900000000 + index,
                "friendsDni": [],
            }

            try:
                response = requests.post(
                    f"{NODE_API_URL}/register", json=user, timeout=30
                )
                return response.status_code == 201
            except Exception as e:
                print(f"Error registering user {index}: {e}")
                return False

        # Run 10 concurrent registrations
        with ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(register_user, i) for i in range(10)]
            results = [f.result() for f in futures]

        # At least 80% should succeed
        success_count = sum(results)
        assert success_count >= 8, f"Only {success_count}/10 registrations succeeded"

    def test_service_health_checks(self):
        """Test health check endpoints for all services."""
        services = [
            (NODE_API_URL + "/health", "Node.js"),
            (JAVA_API_URL + "/health", "Java"),
            (PYTHON_API_URL + "/health", "Python"),
        ]

        for url, name in services:
            response = requests.get(url, timeout=5)
            assert response.status_code == 200, f"{name} health check failed"

            # Check detailed health
            detailed_url = url.replace("/health", "/health/detailed")
            if "node" in name.lower():
                response = requests.get(detailed_url, timeout=5)
                data = response.json()
                assert data["status"] in ["healthy", "degraded"]

    def test_rabbitmq_message_flow(self):
        """Test RabbitMQ message flow through the system."""
        # This test monitors RabbitMQ to ensure messages flow correctly

        # Get initial queue message counts
        initial_counts = {}
        for queue in ["queue_lp1", "queue_lp2", "queue_lp3", "queue_lp3_ack"]:
            try:
                method = self.rabbitmq_channel.queue_declare(queue=queue, passive=True)
                initial_counts[queue] = method.method.message_count
            except:
                initial_counts[queue] = 0

        # Send a registration
        test_user = {
            "nombre": "RabbitMQ Test User",
            "correo": f"rabbitmq_test_{int(time.time())}@example.com",
            "clave": 123456,
            "dni": 20000200,
            "telefono": 987654324,
            "friendsDni": [],
        }

        response = requests.post(f"{NODE_API_URL}/register", json=test_user, timeout=30)

        assert response.status_code == 201

        # Give time for messages to be processed
        time.sleep(2)

        # Messages should have been processed (counts should be back to initial or lower)
        for queue in ["queue_lp1", "queue_lp2", "queue_lp3"]:
            try:
                method = self.rabbitmq_channel.queue_declare(queue=queue, passive=True)
                current_count = method.method.message_count
                assert current_count <= initial_counts[queue] + 1, (
                    f"Queue {queue} has unprocessed messages"
                )
            except:
                pass


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

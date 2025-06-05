#!/bin/bash

# Integration test runner script

echo "==================================="
echo "Running Integration Tests"
echo "==================================="

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "Error: docker-compose is not installed"
    exit 1
fi

# Check if all services are running
echo "Checking service status..."

services=("rabbitmq:5672" "postgres:5432" "mariadb:3306" "java-app:8081" "python-app:8082" "node-app:8083")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if nc -z localhost $port 2>/dev/null; then
        echo "✅ $name is running on port $port"
    else
        echo "❌ $name is not running on port $port"
        echo "Please start all services with: docker-compose up -d"
        exit 1
    fi
done

echo ""
echo "All services are running!"
echo ""

# Install test dependencies if needed
echo "Installing test dependencies..."
pip install pytest requests psycopg2-binary pymysql pika --quiet

# Run the integration tests
echo ""
echo "Running integration tests..."
echo ""

python -m pytest tests/integration/test_e2e_flow.py -v --tb=short

# Check test results
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All integration tests passed!"
else
    echo ""
    echo "❌ Some integration tests failed"
    exit 1
fi 
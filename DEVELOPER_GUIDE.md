# Developer Guide - Distributed Registration System

## System Overview

This project implements a distributed user registration system with DNI validation using RabbitMQ as middleware. The system consists of three microservices in different languages, each with its own database.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   LP3       │     │  RabbitMQ   │     │    LP2      │
│  Node.js    │────▶│  Middleware │────▶│   Python    │
│  (Client)   │◀────│  Port 5672  │◀────│ (Validator) │
└─────────────┘     └─────────────┘     └──────┬──────┘
                            │                    │
                            ▼                    ▼
                    ┌─────────────┐      ┌─────────────┐
                    │    LP1      │      │   MariaDB   │
                    │    Java     │      │    (BD2)    │
                    │ (Persister) │      │  DNI Data   │
                    └──────┬──────┘      └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ PostgreSQL  │
                    │    (BD1)    │
                    │  User Data  │
                    └─────────────┘
```

## Message Flow

1. **Client Request**: LP3 (Node.js) sends registration request via RabbitMQ
2. **Validation**: LP2 (Python) validates DNI and friends exist in BD2
3. **Persistence**: If valid, LP1 (Java) saves user and relationships to BD1
4. **Response**: Confirmation sent back to LP3

## Prerequisites

### Required Software
- **Docker & Docker Compose**: Latest version
- **Java**: JDK 21 (for development)
- **Maven**: 3.8+ (for Java builds)
- **Python**: 3.11+ (3.13.3 installed)
- **Node.js**: 20+ (for LP3 client)
- **Git**: For version control

### System Requirements
- **RAM**: Minimum 8GB (for running all services)
- **Disk**: 10GB free space
- **OS**: Linux/Mac/Windows with WSL2

## Current Progress

### ✅ Completed Tasks
1. **Task 1**: Project structure and dependencies for all services
2. **Task 2**: PostgreSQL setup for Java service (BD1)

### 🔄 In Progress
- **Task 3**: MariaDB setup for Python service (BD2)

### 📋 Pending Tasks
See `.taskmaster/tasks/tasks.json` for complete list (25 tasks total)

## Quick Start Guide

### 1. Start RabbitMQ
```bash
cd rabbitmq
docker-compose up -d
# Access management UI: http://localhost:15672 (admin/admin123)
```

### 2. Start PostgreSQL (BD1) - Already Running
```bash
cd java
docker-compose up -d
# Database: registration_db, Port: 5432
```

### 3. Next: Setup MariaDB (BD2)
```bash
cd python
# Create docker-compose.yml for MariaDB
# Initialize with db.sql
```

### 4. Install Dependencies

**Java:**
```bash
cd java
mvn clean install
```

**Python:**
```bash
cd python
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

**Node.js:**
```bash
cd node
npm install
```

## Development Workflow

### Using Taskmaster
```bash
# View next task
tm next

# View specific task
tm task <id>

# Mark task complete
tm done <id>

# View all tasks
tm tasks
```

### Environment Configuration
1. Copy `.env.example` to `.env` in each service directory
2. Update connection strings if needed
3. Ensure all services can reach RabbitMQ

## Key Technical Decisions

### Databases
- **BD1 (PostgreSQL)**: Stores users and friend relationships
- **BD2 (MariaDB)**: Stores DNI validation data (persona table)

### Message Routing
- Exchange: `registro_bus` (direct type)
- Queues:
  - `queue_lp2`: Validation requests
  - `queue_lp1`: Persistence requests
  - `queue_lp3_ack`: Response acknowledgments

### Ports
- RabbitMQ: 5672 (AMQP), 15672 (Management)
- PostgreSQL: 5432
- MariaDB: 3306
- Java Service: 8081
- Python Service: 8082
- Node.js Client: 8083

## Common Issues & Solutions

### Issue: PostgreSQL foreign key errors
**Solution**: Run `java/init-scripts/01-fix-users.sql` to fix schema

### Issue: RabbitMQ connection refused
**Solution**: Ensure RabbitMQ is running and credentials match .env

### Issue: Port conflicts
**Solution**: Check for running services on required ports

## Testing Strategy

### Unit Tests
- Each service has its own test suite
- Run before committing changes

### Integration Tests
- Test complete flow after all services are running
- Use the performance test module in Node.js

### Performance Test
- Target: 1000 concurrent users
- Goal: <500ms response time (95th percentile)

## Next Developer Action Items

1. **Complete Task 3**: Create MariaDB Docker Compose for Python service
   - Use `python/db.sql` for initialization
   - Ensure persona table has proper indexes on DNI

2. **Start Implementation**: After databases are ready:
   - Task 4-8: Java service implementation
   - Task 9-13: Python service implementation
   - Task 14-18: Node.js client implementation

3. **Documentation**: Update this guide as you progress

## Resources

- Task Details: `.taskmaster/tasks/`
- PRD: `.taskmaster/docs/prd.txt`
- Progress: `.taskmaster/docs/development-progress.md`
- Database Docs: `java/README-DB.md` (similar needed for Python)

## Contact & Support

For questions about:
- Architecture: Review `.taskmaster/docs/prd.txt`
- Tasks: Check individual task files in `.taskmaster/tasks/`
- Database schemas: See respective `db.sql` files

Remember to commit your changes frequently and update the progress documentation! 
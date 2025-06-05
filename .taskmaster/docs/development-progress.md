# Development Progress
THIS DOCUMENT SHOULD BE UPDATED EVERY TIME A NEW TASK IS ADDED OR COMPLETED.

## Task 1: Setup Project Structure and Dependencies ✅

### Completed Actions:

#### Java Service (LP1)
- ✅ Created `pom.xml` with Spring Boot 3.2.0 dependencies
  - Spring Boot Web, JPA, AMQP
  - PostgreSQL driver
  - Lombok, Validation
  - Testing dependencies (JUnit, Mockito, H2)
- ✅ Created Maven directory structure
- ✅ Created `.gitignore` for Java/Maven
- ✅ Created `.env.example` template
- ✅ Verified Maven dependencies resolve successfully

#### Python Service (LP2)
- ✅ Created `requirements.txt` with Flask dependencies
  - Flask 3.0.0, Flask-CORS
  - SQLAlchemy 2.0.23, PyMySQL
  - Pika 1.3.2 for RabbitMQ
  - Testing tools (pytest, coverage)
  - Development tools (black, flake8, mypy)
- ✅ Created Python project structure
- ✅ Created `.gitignore` for Python
- ✅ Created `.env.example` template

#### Node.js Service (LP3)
- ✅ Created `package.json` with Express dependencies
  - Express 4.18.2
  - amqplib 0.10.3 for RabbitMQ
  - Testing tools (Jest, Supertest)
  - Performance monitoring tools
  - Security middleware (helmet, cors)
- ✅ Created Node.js project structure
- ✅ Created `.gitignore` for Node.js
- ✅ Created `.env.example` template

#### General
- ✅ Created root `.gitignore` file
- ✅ All services configured with proper directory structures
- ✅ Environment templates ready for configuration

## Task 2: Create Docker Compose for PostgreSQL (BD1) ✅

### Completed Actions:

- ✅ Created `java/docker-compose.yml` with PostgreSQL 16-alpine
- ✅ Created `java/docker-compose.override.yml` for development settings
- ✅ Created initialization scripts to fix schema issues:
  - `java/init-scripts/01-fix-users.sql` - Adds PRIMARY KEY and creates friend table
  - `java/init-scripts/02-fix-schema.sql` - Additional fixes
- ✅ Created `java/README-DB.md` with database documentation
- ✅ Successfully started PostgreSQL container
- ✅ Database initialized with:
  - 300 users loaded from db.sql
  - 461 friend relationships loaded
  - Proper indexes created for performance

### Issues Resolved:
- Fixed foreign key reference from 'usuario' to 'users' in friend table
- Added PRIMARY KEY constraint to users table
- Manually executed initialization scripts after container creation

## Task 3: Create Docker Compose for MariaDB (BD2) ✅

### Completed Actions:

- ✅ Found existing `python/docker-compose.yml` with MariaDB 11 configuration
- ✅ Fixed network configuration to use external `registration_network`
- ✅ Successfully started MariaDB container
- ✅ Created `python/README-DB.md` with database documentation
- ✅ Database initialized with:
  - 1000 personas loaded from db.sql
  - DNI validation data ready for Python service
  - All columns properly configured (id, dni, nombre, apellidos, lugar_nac, ubigeo, direccion)

### Configuration Details:
- **Container Name**: registration-mariadb
- **Database**: dni_db
- **Port**: 3306
- **Credentials**: dbuser/dbpass123

## Task 4: Implement Java User Service (LP1) - Basic Structure ✅

### Completed Actions:

- ✅ Created main Spring Boot application class (`UserServiceApplication.java`)
- ✅ Created `application.yml` with complete configuration:
  - Server port 8081 with context path `/api`
  - PostgreSQL datasource configuration
  - RabbitMQ connection settings
  - Logging configuration
- ✅ Created RabbitMQ configuration class with:
  - Direct exchange setup (`registro_bus`)
  - Queue definitions (`queue_lp1`, `queue_lp3_ack`)
  - Message converter for JSON
- ✅ Created health check controller endpoint
- ✅ Created `.env` file with correct database credentials
- ✅ Verified application starts successfully on port 8081
- ✅ Health endpoint responds at `http://localhost:8081/api/health`

### Configuration Created:
- **Application Port**: 8081
- **Context Path**: /api
- **RabbitMQ Exchange**: registro_bus (direct)
- **Queues**: queue_lp1 (persist), queue_lp3_ack (response)
- **Routing Keys**: lp1.persist, lp1.persisted

## Current Status

- **RabbitMQ**: ✅ Running on ports 5672/15672
- **PostgreSQL (BD1)**: ✅ Running with 300 users and 461 friend relationships
- **MariaDB (BD2)**: ✅ Running with 1000 personas for DNI validation
- **Java Service (LP1)**: ✅ Basic structure complete, running on port 8081
- **Python Service (LP2)**: Dependencies defined, database running
- **Node.js Service (LP3)**: Dependencies defined

## Next Task: Task 5 - Implement Java User Service - Database Entities

This task involves creating JPA entities for User and Friend relationships.

### Next Steps:
1. Create User entity with all required fields
2. Create Friend entity for many-to-many relationship
3. Create repository interfaces
4. Write unit tests for entities

### Notes:
- Java requires JDK 21 (as specified in pom.xml)
- Node.js requires version 20+ (as specified in package.json)
- Python 3.13.3 is installed and ready 
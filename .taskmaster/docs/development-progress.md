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

## Task 5: Implement Java User Service - Database Entities ✅

### Completed Actions:

- ✅ Created `User` JPA entity with all required fields:
  - Integer id (auto-generated)
  - String nombre (name)
  - String correo (email) with unique constraint
  - Integer clave (password)
  - Integer dni (national ID) with unique constraint
  - Integer telefono (phone)
- ✅ Implemented many-to-many friend relationship:
  - Self-referencing relationship using `friend` join table
  - Bidirectional relationship with `friends` and `friendOf` sets
  - Helper methods `addFriend()` and `removeFriend()` for relationship management
- ✅ Created `UserRepository` interface with custom queries:
  - Find by correo (email) and dni
  - Check existence by correo and dni
  - Fetch users with friends eagerly loaded
  - Find multiple users by DNI list
- ✅ Created comprehensive unit tests:
  - `UserTest`: Tests entity creation, friend relationships, builder pattern
  - `UserRepositoryTest`: Tests all repository methods with H2 in-memory database
- ✅ Configured test environment:
  - Created `application-test.yml` for H2 database configuration
  - All tests passing with proper transaction management

### Technical Decisions:
- Used Lombok for reducing boilerplate code
- Excluded friends from equals/hashCode/toString to avoid circular references
- Used H2 database for testing with PostgreSQL compatibility mode
- Implemented eager fetching queries for performance optimization

## Task 6: Implement Java User Service - RabbitMQ Consumer ✅

### Completed Actions:

- ✅ Created `RegistrationRequest` DTO for incoming messages with all required fields:
  - String nombre (name)
  - String correo (email)
  - Integer clave (password)
  - Integer dni (national ID)
  - Integer telefono (phone)
  - List<Integer> friendsDni (list of friend DNIs)
- ✅ Created `PersistenceResponse` DTO for response messages:
  - Integer dni
  - String status (SUCCESS/FAILED)
  - String message
  - String timestamp
- ✅ Created `RabbitMQConsumer` service with:
  - @RabbitListener annotation for queue_lp1
  - Message validation for all required fields
  - Proper error handling and logging
  - TODO placeholders for tasks 7 and 8
- ✅ Enabled RabbitMQ listeners by adding @EnableRabbit annotation
- ✅ Created comprehensive unit tests:
  - `RabbitMQConsumerTest`: Tests all validation scenarios
  - `RabbitMQIntegrationTest`: Tests Spring context and configuration
- ✅ All tests passing (27 total)
- ✅ Successfully connected to RabbitMQ and listening on queue_lp1

### Technical Decisions:
- Used @RabbitListener for automatic message consumption
- Implemented validation in the consumer to fail fast on invalid messages
- Used Lombok for DTOs to reduce boilerplate
- Structured consumer to easily integrate with persistence logic (task 7)

## Task 7: Implement Java User Service - Persistence Logic ✅

### Completed Actions:

- ✅ Created `UserService` with complete persistence logic:
  - `persistUser()` method that handles the entire registration flow
  - Duplicate checking by both DNI and email
  - Transactional operations using @Transactional
  - Friend relationship management with partial success handling
  - Proper error handling and response building
- ✅ Implemented friend relationship logic:
  - `addFriendRelationships()` method to establish bidirectional relationships
  - Graceful handling of non-existent friends (logs warning, continues)
  - Returns count of successfully added friends
- ✅ Updated `RabbitMQConsumer` to use UserService:
  - Injected UserService dependency
  - Replaced TODO with actual persistence call
  - Added success/failure logging based on response
- ✅ Created comprehensive test coverage:
  - `UserServiceTest`: 11 unit tests covering all scenarios
  - `UserServiceIntegrationTest`: 8 integration tests with real database
  - Updated `RabbitMQConsumerTest` to mock UserService
- ✅ All 48 tests passing

### Technical Decisions:
- Used @Transactional to ensure data consistency
- Friend relationships continue even if some friends don't exist (partial success)
- Response includes timestamp for audit trail
- Integration tests use unique DNIs to avoid conflicts
- Used @Rollback annotation to ensure test isolation

## Current Status

- **RabbitMQ**: ✅ Running on ports 5672/15672
- **PostgreSQL (BD1)**: ✅ Running with 300 users and 461 friend relationships
- **MariaDB (BD2)**: ✅ Running with 1000 personas for DNI validation
- **Java Service (LP1)**: ✅ Consumer ready, persistence logic complete
- **Python Service (LP2)**: Dependencies defined, database running
- **Node.js Service (LP3)**: Dependencies defined

## Next Task: Task 8 - Implement Java User Service - Response Publisher

This task involves implementing RabbitMQ publisher to send persistence confirmations.

### Next Steps:
1. Create RabbitMQ publisher service
2. Send responses to queue_lp3_ack with routing key lp1.persisted
3. Update RabbitMQConsumer to send responses after persistence
4. Handle both success and failure scenarios

### Notes:
- Java requires JDK 21 (as specified in pom.xml)
- Node.js requires version 20+ (as specified in package.json)
- Python 3.13.3 is installed and ready 
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

## Task 8: Implement Java User Service - Response Publisher ✅

### Completed Actions:

- ✅ Created `RabbitMQPublisher` service with complete publishing logic:
  - `publishPersistenceResponse()` method to send responses to RabbitMQ
  - `publishErrorResponse()` helper method for error scenarios
  - `publishSuccessResponse()` helper method for success scenarios
  - Error handling to prevent disrupting main flow if publishing fails
  - Uses RabbitTemplate with JSON message converter
- ✅ Updated `RabbitMQConsumer` to use publisher:
  - Injected RabbitMQPublisher dependency
  - Replaced TODO comments with actual publishing calls
  - Publishes success responses after successful persistence
  - Publishes error responses for validation failures
  - Publishes error responses for unexpected exceptions
- ✅ Created comprehensive test coverage:
  - `RabbitMQPublisherTest`: 5 unit tests covering all publishing scenarios
  - `RabbitMQPublisherIntegrationTest`: 4 integration tests with Spring context
  - Updated `RabbitMQConsumerTest` to verify publisher interactions
- ✅ All 57 tests passing
- ✅ Application running successfully on port 8081
- ✅ Health endpoint responding at `http://localhost:8081/api/health`

### Technical Decisions:
- Publisher doesn't throw exceptions to avoid disrupting the main flow
- Used builder pattern for creating response objects
- Timestamp format uses ISO_LOCAL_DATE_TIME for consistency
- Integration tests use mocked RabbitTemplate to avoid requiring actual RabbitMQ
- Error messages include context (e.g., "Invalid request: " or "Unexpected error: ")

### Message Flow Implemented:
- Consumer receives message from `queue_lp1`
- Validates and persists user data
- Publisher sends response to exchange `registro_bus` with routing key `lp1.persisted`
- Response will be routed to `queue_lp3_ack` for Node.js client consumption

## Task 9: Implement Python Validator Service (LP2) - Basic Structure ✅

### Completed Actions:

- ✅ Created Flask application structure with proper configuration:
  - `config/config.py`: Complete configuration for Flask, SQLAlchemy, and RabbitMQ
  - `config/database.py`: Database connection manager with SQLAlchemy
  - `config/rabbitmq.py`: RabbitMQ connection manager with pika
  - `app.py`: Main Flask application with health check endpoints
- ✅ Set up environment configuration:
  - Created `.env` file from template
  - Updated database credentials to match docker-compose settings
- ✅ Resolved dependencies:
  - Fixed flask-healthz incompatibility with Flask 3.0
  - Upgraded SQLAlchemy to 2.0.35 for Python 3.13 compatibility
  - Created virtual environment and installed all dependencies
- ✅ Successfully started Flask application on port 8082
- ✅ Health check endpoint responding at `http://localhost:8082/api/health`
- ✅ Root endpoint responding at `http://localhost:8082/`

### Technical Decisions:
- Used Flask instead of FastAPI for consistency with requirements.txt
- Removed flask-healthz due to Flask 3.0 incompatibility, implemented manual health checks
- Used SQLAlchemy 2.0.35 for Python 3.13 compatibility
- Configured proper connection pooling for both database and RabbitMQ
- Set up logging with Flask's built-in logging configuration

### Current Service Status:
- Flask app running successfully on port 8082
- Health endpoint reports "degraded" status (expected, as DB models and RabbitMQ consumers not yet implemented)
- Ready for next tasks: database models and RabbitMQ integration

## Task 10: Implement Python Validator Service - Database Models ✅

### Completed Actions:

- ✅ Created `models/persona.py` with complete Persona model:
  - All fields mapped correctly: id, dni, nombre, apellidos, lugar_nac, ubigeo, direccion
  - Added index on dni field for performance
  - Implemented `__repr__` method for debugging
  - Implemented `to_dict()` method for serialization
- ✅ Created `models/persona_repository.py` with repository pattern:
  - `find_by_dni()`: Find single persona by DNI
  - `find_multiple_by_dni()`: Find multiple personas by DNI list
  - `exists_by_dni()`: Check if DNI exists
  - `check_multiple_exist()`: Check which DNIs exist from a list
  - `count()`: Count total personas
- ✅ Fixed SQLAlchemy compatibility issues:
  - Upgraded to SQLAlchemy 2.0.35 for Python 3.13 compatibility
  - Fixed `text()` import for raw SQL execution
- ✅ Created comprehensive tests:
  - `test_persona_model.py`: 5 unit tests for model functionality
  - `test_database_integration.py`: 5 integration tests with MariaDB
- ✅ Verified database connection and queries:
  - Successfully connected to MariaDB
  - Confirmed 1000 personas loaded in database
  - Tested querying by single and multiple DNIs

### Technical Decisions:
- Used repository pattern for clean data access layer
- Added index on dni column for query performance
- Implemented efficient batch queries for multiple DNI lookups
- Used pytest fixtures for test database sessions

### Test Results:
- All 5 unit tests passing
- All 5 integration tests passing
- Verified data integrity with existing database

## Task 11: Implement Python Validator Service - RabbitMQ Consumer ✅

### Completed Actions:

- ✅ Created `validator/dto.py` with complete data transfer objects:
  - `ValidationRequest`: Represents incoming validation requests from LP3
  - `ValidationResponse`: Represents validation responses to be sent
  - Both DTOs include JSON serialization/deserialization methods
  - Added proper field validation and default values
- ✅ Created `validator/rabbitmq_consumer.py` with consumer implementation:
  - `ValidationConsumer` class with message processing logic
  - Proper message acknowledgment and error handling
  - Message parsing and validation
  - QoS settings for reliable processing
  - TODO placeholders for validation logic (Task 12) and publishing (Task 13)
- ✅ Created `consumer_thread.py` for background processing:
  - Thread wrapper to run consumer in background
  - Graceful shutdown support
  - Integration with Flask application lifecycle
- ✅ Integrated consumer into Flask application:
  - Consumer thread starts automatically when RabbitMQ is connected
  - Added consumer health check to `/api/health` endpoint
  - Automatic cleanup on application shutdown
- ✅ Created comprehensive test coverage:
  - `test_dto.py`: 7 unit tests for DTOs
  - `test_rabbitmq_consumer.py`: 8 unit tests for consumer logic
  - All tests passing with proper mocking

### Technical Decisions:
- Used dataclasses for DTOs to reduce boilerplate
- Implemented consumer in a separate thread to not block Flask
- Used manual acknowledgments for reliable message processing
- Reject and requeue messages on parsing failures
- Added consumer status to health endpoint for monitoring

### Message Processing Flow:
1. Consumer receives message from `queue_lp2` (lp2.validate)
2. Parses and validates the JSON message
3. Creates ValidationRequest object
4. TODO (Task 12): Perform DNI validation
5. TODO (Task 13): Publish response
6. Acknowledges message if successful

## Task 14: Implement Node.js Client (LP3) - Basic Structure ✅

### Completed Actions:

- ✅ Created `src/config/config.js` with complete configuration management
- ✅ Created `src/utils/logger.js` with Winston logging setup
- ✅ Created `src/index.js` main Express application with:
  - Express server on port 8083
  - Middleware: helmet, CORS, rate limiting, morgan logging
  - Error handling and graceful shutdown
  - RabbitMQ connection initialization
- ✅ Created `.env` file with proper configurations
- ✅ All dependencies from package.json are ready to use

## Task 15: Implement Node.js Client - RabbitMQ Publisher ✅

### Completed Actions:

- ✅ Created `src/services/rabbitmq.js` with RabbitMQService class
- ✅ Implemented `publishRegistration()` method that:
  - Publishes to 'lp3.signup' routing key
  - Uses correlation IDs for request tracking
  - Implements timeout handling
  - Returns promise that resolves with response
- ✅ Configured exchange and queue setup
- ✅ Proper error handling and connection management

## Task 16: Implement Node.js Client - Response Consumer ✅

### Completed Actions:

- ✅ Implemented consumer in `src/services/rabbitmq.js`
- ✅ Created `handleAcknowledgment()` method that:
  - Consumes from 'queue_lp3_ack'
  - Correlates responses with requests using correlation IDs
  - Resolves pending promises with response data
  - Handles timeouts and errors
- ✅ Integrated with publisher for complete request-response flow

## Task 17: Implement Node.js Client - REST API Endpoints ✅

### Completed Actions:

- ✅ Created `src/routes/register.js` with registration endpoints
- ✅ Implemented `POST /api/register` endpoint with:
  - Comprehensive input validation using express-validator
  - Async request handling via RabbitMQ
  - Proper error responses and status codes
  - Timeout handling
- ✅ Implemented `GET /api/register/status` for service status
- ✅ Created `src/routes/health.js` with health check endpoints

## Task 18: Implement Node.js Client - Performance Test Module ✅

### Completed Actions:

- ✅ Created `src/performance-test.js` performance testing module
- ✅ Implemented concurrent request generation with:
  - Configurable batch processing (50 users per batch)
  - 1000 total users simulation
  - Metrics collection (response times, success rates)
  - P50, P95, P99 percentile calculations
  - Progress tracking and reporting
- ✅ Added performance test script to package.json
- ✅ Validation against <500ms P95 requirement

### Technical Decisions:
- Used ES modules (type: "module" in package.json)
- Implemented correlation-based request-response tracking
- Used singleton pattern for RabbitMQ service
- Batch processing for performance tests to avoid overwhelming the system

## Current Status

- **RabbitMQ**: ✅ Running on ports 5672/15672
- **PostgreSQL (BD1)**: ✅ Running with 300 users and 461 friend relationships
- **MariaDB (BD2)**: ✅ Running with 1000 personas for DNI validation
- **Java Service (LP1)**: ✅ Complete - Consumer, persistence, and publisher implemented
- **Python Service (LP2)**: ✅ Flask structure, database models, and RabbitMQ consumer implemented (Tasks 12-13 pending)
- **Node.js Service (LP3)**: ✅ Complete - All components implemented (Tasks 14-18)

## Next Task: Task 12 - Implement Python Validator Service - Validation Logic

This task involves implementing the DNI and friends validation logic.

### Next Steps:
1. Create validation service to check DNI existence
2. Validate all friend DNIs in the list
3. Return detailed validation results
4. Handle edge cases and errors

### Notes:
- Java requires JDK 21 (as specified in pom.xml)
- Node.js requires version 20+ (as specified in package.json)
- Python 3.13.3 with virtual environment is set up and ready
- Node.js service is fully implemented and ready for integration testing once Python service is complete 
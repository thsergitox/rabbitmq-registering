# Development Plan - Distributed User Registration System

## Project Overview
A distributed system using RabbitMQ as middleware to connect three microservices:
- **LP1 (Java)**: User persistence service with PostgreSQL
- **LP2 (Python)**: DNI validation service with MariaDB  
- **LP3 (Node.js)**: Client application and performance testing

## Task Breakdown (25 Tasks Total)

### Phase 1: Infrastructure Setup (Tasks 1-3)
- **Task 1**: Setup project structure and dependencies
- **Task 2**: Create Docker Compose for PostgreSQL (BD1)
- **Task 3**: Create Docker Compose for MariaDB (BD2)

### Phase 2: Java Service Implementation (Tasks 4-8)
- **Task 4**: Java service basic structure (Spring Boot)
- **Task 5**: Database entities (User, Friend)
- **Task 6**: RabbitMQ consumer implementation
- **Task 7**: User persistence business logic
- **Task 8**: Response publisher to RabbitMQ

### Phase 3: Python Service Implementation (Tasks 9-13)
- **Task 9**: Python service basic structure (Flask/FastAPI)
- **Task 10**: Database models (Persona)
- **Task 11**: RabbitMQ consumer for validation
- **Task 12**: DNI validation logic
- **Task 13**: Response publisher for validation results

### Phase 4: Node.js Client Implementation (Tasks 14-18)
- **Task 14**: Express.js basic structure
- **Task 15**: RabbitMQ publisher for requests
- **Task 16**: Response consumer implementation
- **Task 17**: REST API endpoints
- **Task 18**: Performance test module (1000 users)

### Phase 5: Deployment & Quality (Tasks 19-25)
- **Task 19**: Docker Compose for all services
- **Task 20**: Error handling and retry logic
- **Task 21**: Thread safety and connection pooling
- **Task 22**: Integration tests
- **Task 23**: Performance testing and optimization
- **Task 24**: System documentation
- **Task 25**: Monitoring and logging setup

## Key Technical Decisions

### Message Flow
1. LP3 → RabbitMQ: `lp3.signup`
2. RabbitMQ → LP2: `lp2.validate`
3. LP2 → RabbitMQ: `lp2.query.ok/fail`
4. RabbitMQ → LP1: `lp1.persist` (if OK)
5. LP1 → RabbitMQ: `lp1.persisted`
6. RabbitMQ → LP3: `lp3.ack`

### Technology Stack
- **Java**: Spring Boot, JPA, PostgreSQL driver, Spring AMQP
- **Python**: Flask/FastAPI, SQLAlchemy, pika, pymysql
- **Node.js**: Express, amqplib, axios
- **Databases**: PostgreSQL 16, MariaDB 11
- **Message Broker**: RabbitMQ 3.13 with management

### Ports Configuration
- RabbitMQ: 5672 (AMQP), 15672 (Management)
- LP1 (Java): 8081
- LP2 (Python): 8082
- LP3 (Node.js): 8083
- PostgreSQL: 5432
- MariaDB: 3306

## Next Steps

1. **Start RabbitMQ** (already configured):
   ```bash
   cd rabbitmq
   docker-compose up -d
   ```

2. **Begin with Task 1**: Setup project dependencies
   - Create pom.xml for Java
   - Create requirements.txt for Python
   - Create package.json for Node.js

3. **Follow task dependencies**: Tasks are ordered by dependencies

## Development Workflow

1. Check next task: `tm next`
2. View task details: `tm task <id>`
3. Mark task complete: `tm done <id>`
4. Check progress: `tm tasks`

## Testing Strategy

Each task includes specific test criteria. Key testing milestones:
- Individual service unit tests
- Integration tests for message flow
- Performance test with 1000 concurrent users
- End-to-end system validation

## Success Criteria

- Handle 1000 concurrent registrations
- Response time < 500ms (95th percentile)
- Zero message loss
- Proper error handling and recovery
- Complete documentation 
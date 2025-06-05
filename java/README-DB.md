# PostgreSQL Database Setup for Java Service (LP1)

## Overview
This directory contains the PostgreSQL database configuration for the User Service (LP1).

## Database Details
- **Database Name**: registration_db
- **Port**: 5432
- **User**: postgres
- **Password**: postgres123

## Quick Start

### 1. Start the Database
```bash
docker-compose up -d
```

### 2. Verify Database is Running
```bash
docker-compose ps
docker-compose logs postgres-bd1
```

### 3. Connect to Database
```bash
# Using docker exec
docker exec -it registration-postgres psql -U postgres -d registration_db

# Using psql client
psql -h localhost -p 5432 -U postgres -d registration_db
```

## Database Schema

### Tables
1. **users** - Main user information
   - id (INT) - Primary key
   - nombre (VARCHAR 512)
   - correo (VARCHAR 512)
   - clave (INT)
   - dni (INT)
   - telefono (INT)

2. **friend** - Friend relationships (many-to-many)
   - user_id (INT) - Foreign key to users.id
   - friend_id (INT) - Foreign key to users.id
   - Primary key: (user_id, friend_id)

### Sample Data
The database is initialized with:
- 300 users with sample data
- Multiple friend relationships

## Useful Commands

### View Tables
```sql
\dt                    -- List all tables
\d users              -- Describe users table
\d friend             -- Describe friend table
```

### Query Examples
```sql
-- Find all users
SELECT * FROM users LIMIT 10;

-- Find user by DNI
SELECT * FROM users WHERE dni = 58889613;

-- Find all friends of a user
SELECT u.* FROM users u 
JOIN friend f ON u.id = f.friend_id 
WHERE f.user_id = 1;

-- Count users and relationships
SELECT COUNT(*) as total_users FROM users;
SELECT COUNT(*) as total_friendships FROM friend;
```

### Backup and Restore
```bash
# Backup
docker exec registration-postgres pg_dump -U postgres registration_db > backup.sql

# Restore
docker exec -i registration-postgres psql -U postgres registration_db < backup.sql
```

## Troubleshooting

### Reset Database
```bash
# Stop and remove container and volume
docker-compose down -v

# Start fresh
docker-compose up -d
```

### View Logs
```bash
docker-compose logs -f postgres-bd1
```

### Connection Issues
- Ensure port 5432 is not already in use
- Check firewall settings
- Verify Docker is running 
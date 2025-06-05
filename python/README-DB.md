# MariaDB Database Documentation (BD2)

## Overview
This database stores DNI validation data for the Python validation service (LP2).

## Database Details
- **Database Name**: dni_db
- **Port**: 3306
- **Container Name**: registration-mariadb
- **Image**: MariaDB 11

## Connection Details
```
Host: localhost
Port: 3306
Database: dni_db
Username: dbuser
Password: dbpass123
Root Password: mariadb123
```

## Schema

### Persona Table
Stores DNI information for validation:

| Column     | Type         | Description                |
|------------|--------------|----------------------------|
| id         | int(11)      | Person ID                  |
| dni        | int(11)      | National Identity Document |
| nombre     | varchar(512) | First name                 |
| apellidos  | varchar(512) | Last names                 |
| lugar_nac  | varchar(512) | Place of birth             |
| ubigeo     | int(11)      | Geographic code            |
| direccion  | varchar(512) | Address                    |

## Data
- **Total Records**: 1000 personas with valid DNI data
- **Source**: Initialized from `db.sql` file

## Docker Commands

### Start Database
```bash
cd python
docker-compose up -d
```

### Stop Database
```bash
docker-compose down
```

### Access Database
```bash
docker exec -it registration-mariadb mariadb -udbuser -pdbpass123 dni_db
```

### Check Data
```bash
# Count personas
docker exec registration-mariadb mariadb -udbuser -pdbpass123 -e "USE dni_db; SELECT COUNT(*) FROM persona;"

# Sample data
docker exec registration-mariadb mariadb -udbuser -pdbpass123 -e "USE dni_db; SELECT * FROM persona LIMIT 5;"
```

## Usage in Python Service
The Python service will use this database to:
1. Validate that a given DNI exists in the persona table
2. Validate that all friend DNIs also exist
3. Return validation results to the Java service via RabbitMQ

## Performance Considerations
- DNI column should have an index for fast lookups
- Connection pooling recommended for high concurrency
- Use prepared statements to prevent SQL injection 
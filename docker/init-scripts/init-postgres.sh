#!/bin/bash
# PostgreSQL Initialization Script
# This script creates multiple databases for different microservices

# Enable strict error handling
set -e  # Exit on any error
set -u  # Exit on undefined variable

# Function to create a database if it doesn't exist
create_database() {
    local database=$1
    echo "Creating database: $database"
    
    # Use psql to create database
    # -v ON_ERROR_STOP=1: Stop on first error
    # -U postgres: Connect as postgres user
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        -- Create database if it doesn't exist
        SELECT 'CREATE DATABASE $database'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database')\gexec
        
        -- Grant all privileges to postgres user
        GRANT ALL PRIVILEGES ON DATABASE $database TO postgres;
        
        -- Log success
        \echo 'Database $database created successfully'
EOSQL
}

# Main execution
echo "Starting PostgreSQL database initialization..."

# Create database for Auth Service
create_database "auth_db"

# Create database for User Service
create_database "user_db"

# Create database for Order Service
create_database "order_db"

# Create database for Payment Service
create_database "payment_db"

echo "PostgreSQL initialization completed successfully!"
echo "Created databases: auth_db, user_db, order_db, payment_db"


#!/bin/bash

# Init script for the database

sudo service postgresql start
sudo -u postgres dropdb blingbankdb
sudo -u postgres createdb blingbankdb
sudo -u postgres psql -d blingbankdb -c "CREATE USER blingbankadmin WITH SUPERUSER LOGIN PASSWORD 'blingbankadmin';"

# Define the paths to the PostgreSQL configuration files
PG_CONF_FILE="/etc/postgresql/16/main/postgresql.conf"
PG_HBA_FILE="/etc/postgresql/16/main/pg_hba.conf"

# Backup the original configuration files
sudo cp "$PG_CONF_FILE" "$PG_CONF_FILE.backup"
sudo cp "$PG_HBA_FILE" "$PG_HBA_FILE.backup"

# Update postgresql.conf to allow PostgreSQL to listen on all available IP addresses
sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/g" "$PG_CONF_FILE"

# Update pg_hba.conf to allow all connections
sudo sed -i "s/host    all             all             127.0.0.1\/32            md5/host    all             all             0.0.0.0\/0            md5/g" "$PG_HBA_FILE"
sudo sed -i "s/host    all             all             127.0.0.1\/32            scram-sha-256/host    all             all             0.0.0.0\/0            md5/g" "$PG_HBA_FILE"

sudo service postgresql restart

# Configure ip
sudo ifconfig eth0 192.168.1.1/24 up
sudo ip route add default via 192.168.1.254

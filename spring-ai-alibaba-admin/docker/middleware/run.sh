#!/bin/bash

# Check if .env file already exists
if [ -f .env ]; then
    echo ".env file already exists. Skipping creation."
    echo "If you want to regenerate it, please delete .env first."
else
    # Create .env file with complete configuration
    cat > .env << EOF
# User ID and Group ID for containers
UID=$(id -u)
GID=$(id -g)

# Timezone
TZ=Asia/Shanghai

# Default values
MIDDLEWARE_HOME=.
EOF
    echo "Created .env file with UID=$(id -u) and GID=$(id -g)"
fi

# Set permissions for data directories (only if they exist)
if [ -d elasticsearch/data ]; then
    sudo chmod -R 777 elasticsearch/data 2>/dev/null || chmod -R 777 elasticsearch/data
fi

if [ -d kibana/data ]; then
    sudo chmod -R 777 kibana/data 2>/dev/null || chmod -R 777 kibana/data
fi

# Start containers with docker compose
echo "Starting containers with docker compose..."
docker compose up -d --build

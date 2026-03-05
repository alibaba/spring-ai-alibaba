#!/bin/bash

# Parse mode parameter (default: dev)
MODE=${1:-dev}

# Validate mode
if [ "$MODE" != "dev" ] && [ "$MODE" != "prod" ]; then
    echo "Error: Invalid mode '$MODE'. Use 'dev' or 'prod'."
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

echo "Starting middleware services in $MODE mode..."

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
echo "Starting containers with docker-compose-${MODE}.yaml..."
docker compose -f docker-compose-${MODE}.yaml up -d --build

echo ""
echo "Middleware services started successfully in $MODE mode!"
echo ""
if [ "$MODE" = "dev" ]; then
    echo "Dev mode includes: MySQL"
else
    echo "Prod mode includes: MySQL, Redis, Elasticsearch, Kibana, Nacos, RocketMQ, LoongCollector"
fi
echo ""
echo "To stop services, run: ./stop.sh $MODE"

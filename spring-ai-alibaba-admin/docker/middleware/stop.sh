#!/bin/bash

# Parse mode parameter (default: dev)
MODE=${1:-dev}

# Validate mode
if [ "$MODE" != "dev" ] && [ "$MODE" != "prod" ]; then
    echo "Error: Invalid mode '$MODE'. Use 'dev' or 'prod'."
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

# Stop containers with docker compose
echo "Stopping $MODE mode containers with docker compose..."
docker compose -f docker-compose-${MODE}.yaml down

echo ""
echo "Middleware services stopped successfully!"
echo ""
echo "To remove all data, run: docker compose -f docker-compose-${MODE}.yaml down -v"

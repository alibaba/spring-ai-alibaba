#!/bin/bash

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

sudo chmod -R 777 elasticsearch/data
sudo chmod -R 777 kibana/data

echo "Created .env file with UID=$(id -u) and GID=$(id -g)"

# check params
INSTALL_HIGRESS=false
INSTALL_NACOS=false

# parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --install-higress) INSTALL_HIGRESS=true ;;
        --install-nacos) INSTALL_NACOS=true ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# check if need to install Nacos
if [ "$INSTALL_NACOS" = "true" ]; then
    echo "Installing middlewares (including Nacos)..."
    # Run docker compose
    docker compose up --profile nacos -d

    # Check docker compose results
    if [ $? -ne 0 ]; then
        echo "Failed to start Docker Compose. Exiting..."
        exit 1
    fi
else
    echo "Installing middlewares (skipping Nacos)..."
    # start docker compose
    docker compose up -d
    # check docker compose results
    if [ $? -ne 0 ]; then
        echo "Failed to start Docker Compose. Exiting..."
        exit 1
    fi
fi

# check if need to install Higress
if [ "$INSTALL_HIGRESS" = "true" ]; then
    echo "Installing Higress..."
    curl -fsSL https://higress.io/standalone/get-higress.sh | bash -s -- -c file:///opt/higress/conf -a
else
    echo "Skipping Higress installation."
fi

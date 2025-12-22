#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

echo -e "${GREEN}Starting deployment to Kubernetes...${NC}"

# Step 1: Create namespace
echo -e "${YELLOW}Step 1: Creating namespace...${NC}"
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"

# Step 2: Create MySQL init scripts ConfigMap if SQL files exist
# This must be done BEFORE deploying MySQL, so the init scripts are available on first startup
if [ -d "$PROJECT_ROOT/docker/middleware/init/mysql" ]; then
    echo -e "${YELLOW}Step 2: Creating MySQL initialization scripts ConfigMap...${NC}"
    if [ -f "$PROJECT_ROOT/docker/middleware/init/mysql/admin-schema.sql" ] && [ -f "$PROJECT_ROOT/docker/middleware/init/mysql/agentscope-schema.sql" ]; then
        kubectl create configmap mysql-init-scripts \
            --from-file=admin-schema.sql="$PROJECT_ROOT/docker/middleware/init/mysql/admin-schema.sql" \
            --from-file=agentscope-schema.sql="$PROJECT_ROOT/docker/middleware/init/mysql/agentscope-schema.sql" \
            -n spring-ai-admin \
            --dry-run=client -o yaml | kubectl apply -f -
        echo "  - MySQL init scripts ConfigMap created/updated"
    else
        echo -e "${YELLOW}  - Warning: MySQL init SQL files not found, skipping ConfigMap creation${NC}"
    fi
else
    echo -e "${YELLOW}Step 2: Skipping MySQL init scripts (directory not found)${NC}"
fi

# Step 3: Deploy middleware
echo -e "${YELLOW}Step 3: Deploying middleware services...${NC}"

echo "  - Deploying MySQL..."
kubectl apply -f "$SCRIPT_DIR/middleware/mysql/"

echo "  - Deploying Elasticsearch..."
kubectl apply -f "$SCRIPT_DIR/middleware/elasticsearch/"

echo "  - Deploying Kibana..."
kubectl apply -f "$SCRIPT_DIR/middleware/kibana/"

echo "  - Deploying Redis..."
kubectl apply -f "$SCRIPT_DIR/middleware/redis/"

echo "  - Deploying Nacos..."
kubectl apply -f "$SCRIPT_DIR/middleware/nacos/"

echo "  - Deploying RocketMQ..."
kubectl apply -f "$SCRIPT_DIR/middleware/rocketmq/"

echo "  - Deploying LoongCollector..."
kubectl apply -f "$SCRIPT_DIR/middleware/loongcollector/"

# Step 4: Wait for critical services
echo -e "${YELLOW}Step 4: Waiting for critical services to be ready...${NC}"
echo "  - Waiting for MySQL..."
kubectl wait --for=condition=ready pod -l app=mysql -n spring-ai-admin --timeout=300s || echo -e "${RED}Warning: MySQL not ready within timeout${NC}"

echo "  - Waiting for Elasticsearch..."
kubectl wait --for=condition=ready pod -l app=elasticsearch -n spring-ai-admin --timeout=300s || echo -e "${RED}Warning: Elasticsearch not ready within timeout${NC}"

# Step 5: Deploy applications
echo -e "${YELLOW}Step 5: Deploying application services...${NC}"
echo "  - Deploying backend..."
kubectl apply -f "$SCRIPT_DIR/backend/"

echo "  - Deploying frontend..."
kubectl apply -f "$SCRIPT_DIR/frontend/"

# Step 6: Deploy Ingress
echo -e "${YELLOW}Step 6: Deploying Ingress...${NC}"
kubectl apply -f "$SCRIPT_DIR/ingress.yaml"

# Step 7: Show status
echo -e "${GREEN}Deployment completed!${NC}"
echo ""
echo -e "${YELLOW}Current status:${NC}"
kubectl get all -n spring-ai-admin

echo ""
echo -e "${YELLOW}Ingress:${NC}"
kubectl get ingress -n spring-ai-admin

echo ""
echo -e "${GREEN}To view logs, use:${NC}"
echo "  kubectl logs -f deployment/spring-ai-admin-server -n spring-ai-admin"
echo "  kubectl logs -f deployment/frontend -n spring-ai-admin"


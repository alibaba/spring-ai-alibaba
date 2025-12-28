#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo -e "${YELLOW}Starting undeployment from Kubernetes...${NC}"

# Confirm deletion
read -p "Are you sure you want to delete all resources? This will delete all data! (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo -e "${RED}Undeployment cancelled.${NC}"
    exit 1
fi

# Step 1: Delete Ingress
echo -e "${YELLOW}Step 1: Deleting Ingress...${NC}"
kubectl delete -f "$SCRIPT_DIR/ingress.yaml" --ignore-not-found=true

# Step 2: Delete applications
echo -e "${YELLOW}Step 2: Deleting application services...${NC}"
kubectl delete -f "$SCRIPT_DIR/frontend/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/backend/" --ignore-not-found=true

# Step 3: Delete middleware
echo -e "${YELLOW}Step 3: Deleting middleware services...${NC}"
kubectl delete -f "$SCRIPT_DIR/middleware/loongcollector/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/rocketmq/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/nacos/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/redis/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/kibana/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/elasticsearch/" --ignore-not-found=true
kubectl delete -f "$SCRIPT_DIR/middleware/mysql/" --ignore-not-found=true

# Step 4: Delete namespace (this will delete all resources in the namespace)
echo -e "${YELLOW}Step 4: Deleting namespace...${NC}"
read -p "Do you want to delete the namespace and all PVCs? This will delete all persistent data! (yes/no): " delete_ns
if [ "$delete_ns" == "yes" ]; then
    kubectl delete namespace spring-ai-admin --ignore-not-found=true
    echo -e "${GREEN}Namespace deleted.${NC}"
else
    echo -e "${YELLOW}Namespace kept. You can delete it manually with: kubectl delete namespace spring-ai-admin${NC}"
fi

echo -e "${GREEN}Undeployment completed!${NC}"


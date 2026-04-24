##@ Common
SHELL:=/bin/bash

# Image tag from git
IMAGE_TAG := $(shell git describe --tags --always --dirty)

# Docker registry (can be overridden with make image-push REGISTRY=your-registry)
REGISTRY ?=

# Build images locally or pull from remote (default: false - pull from remote)
BUILD_LOCAL ?= false

# Image names with optional registry prefix
FRONTEND_IMAGE := $(if $(REGISTRY),$(REGISTRY)/)spring-ai-admin-frontend
BACKEND_IMAGE := $(if $(REGISTRY),$(REGISTRY)/)spring-ai-admin-server

# Log the running target
LOG_TARGET = echo -e "\033[0;32m==================> Running $@ ============> ... \033[0m"
# Log debugging info
define log
echo -e "\033[36m==================>$1\033[0m"
endef
# Log error info
define errorLog
echo -e "\033[0;31m==================>$1\033[0m"
endef

.PHONY: help
help:
help: ## Show this help message.
	@echo -e "\033[1;3;34m基于 Spring AI Alibaba 的 AI Agent 开发与评估平台.\033[0m\n"
	@echo -e "Usage:\n  make \033[36m<Target>\033[0m \033[36m<Option>\033[0m\n\nTargets:"
	@awk 'BEGIN {FS = ":.*##"; printf ""} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

.PHONY: docker-check
docker-check:
docker-check: ## Check if Docker is installed and running.
	@$(call LOG_TARGET)
	if ! command -v docker &> /dev/null; then \
		@$(call errorLog, Docker is not installed. Please install Docker to proceed.); \
		exit 1; \
	fi
	if ! docker info > /dev/null 2>&1; then \
		@$(call errorLog, Docker is not running. Please start Docker to proceed.); \
		exit 1; \
	fi

##@ Docker Image

.PHONY: build-image
build-image: ## Build the Docker image for the Spring AI Alibaba Admin application.
build-image: docker-check
	@$(call LOG_TARGET)
	@$(call log, Building images with tag: $(IMAGE_TAG))
	@$(call log, start frontend build...)
	cd frontend && docker build -t $(FRONTEND_IMAGE):$(IMAGE_TAG) .
	@$(call log, start backend build...)
	docker build -f spring-ai-alibaba-admin-server-start/Dockerfile -t $(BACKEND_IMAGE):$(IMAGE_TAG) .

.PHONY: image-push
image-push: build-image ## Push the Docker images to a container registry. Usage: make image-push REGISTRY=your-registry
	@$(call LOG_TARGET)
	@if [ -z "$(REGISTRY)" ]; then \
		@$(call errorLog, REGISTRY is not specified. Usage: make image-push REGISTRY=your-registry); \
		exit 1; \
	fi
	@$(call log, Pushing images to $(REGISTRY)...)
	docker push $(FRONTEND_IMAGE):$(IMAGE_TAG)
	docker push $(BACKEND_IMAGE):$(IMAGE_TAG)

##@ Kubernetes

.PHONY: deploy-k8s
deploy-k8s: ## Deploy the Spring AI Alibaba Admin application to a Kubernetes cluster. Usage: make deploy-k8s BUILD_LOCAL=true/false REGISTRY=your-registry
	$(call LOG_TARGET)
	@if [ "$(BUILD_LOCAL)" = "true" ]; then \
		@$(call log, Building images locally for Kubernetes...); \
		@$(MAKE) build-image; \
		export IMAGE_PULL_POLICY=IfNotPresent; \
	else \
		if [ -z "$(REGISTRY)" ]; then \
			@$(call errorLog, REGISTRY is required when BUILD_LOCAL=false. Usage: make deploy-k8s REGISTRY=your-registry); \
			exit 1; \
		fi; \
		@$(call log, Using remote images from $(REGISTRY)...); \
		export IMAGE_PULL_POLICY=Always; \
	fi; \
	@$(call log, Deploying to Kubernetes cluster...); \
	export FRONTEND_IMAGE=$(FRONTEND_IMAGE); \
	export BACKEND_IMAGE=$(BACKEND_IMAGE); \
	export IMAGE_TAG=$(IMAGE_TAG); \
	cd deploy/kubernetes && bash deploy.sh

.PHONY: undeploy-k8s
undeploy-k8s: ## Undeploy the Spring AI Alibaba Admin application from Kubernetes cluster.
	@$(call LOG_TARGET)
	@$(call log, Undeploying from Kubernetes cluster...)
	cd deploy/kubernetes && bash undeploy.sh

##@ Docker Compose

.PHONY: deploy-compose
deploy-compose: docker-check
deploy-compose: ## Deploy the Spring AI Alibaba Admin application using Docker Compose. Usage: make deploy-compose BUILD_LOCAL=true/false REGISTRY=your-registry
	@$(call LOG_TARGET)
	@if [ "$(BUILD_LOCAL)" = "true" ]; then \
		@$(call log, Building images locally...); \
		@$(MAKE) build-image; \
	else \
		if [ -z "$(REGISTRY)" ]; then \
			@$(call errorLog, REGISTRY is required when BUILD_LOCAL=false. Usage: make deploy-compose REGISTRY=your-registry); \
			exit 1; \
		fi; \
		@$(call log, Pulling images from $(REGISTRY)...); \
		docker pull $(FRONTEND_IMAGE):$(IMAGE_TAG); \
		docker pull $(BACKEND_IMAGE):$(IMAGE_TAG); \
	fi
	@$(call log, Starting services with docker-compose...)
	export FRONTEND_IMAGE=$(FRONTEND_IMAGE) && \
	export BACKEND_IMAGE=$(BACKEND_IMAGE) && \
	export IMAGE_TAG=$(IMAGE_TAG) && \
	export UID=$$(id -u) && \
	export GID=$$(id -g) && \
	cd deploy/docker-compose && docker-compose -f docker-compose-service.yaml up -d

.PHONY: undeploy-compose
undeploy-compose: docker-check ## Undeploy the Spring AI Alibaba Admin application from Docker Compose.
	$(call LOG_TARGET)
	$(call log, Stopping and removing services...)
	cd deploy/docker-compose && docker-compose -f docker-compose-service.yaml down
	$(call log, Services stopped successfully)

.PHONY: undeploy-compose-clean
undeploy-compose-clean: docker-check ## Undeploy and clean all data (WARNING: This will delete all data!)
	@$(call LOG_TARGET)
	@read -p "This will delete all data! Are you sure? (yes/no): " confirm; \
	if [ "$$confirm" = "yes" ]; then \
		@$(call log, Stopping services and removing volumes...); \
		cd deploy/docker-compose && docker-compose -f docker-compose-service.yaml down -v; \
		@$(call log, Removing data directory...); \
		rm -rf deploy/docker-compose/data; \
		@$(call log, Cleanup completed); \
	else \
		@$(call errorLog, Cleanup cancelled); \
	fi

.PHONY: env-start
env-start: ## Start the environment services. Usage: make env-start MODE=dev/prod (default: dev)
	@$(call LOG_TARGET)
	@MODE=$${MODE:-dev}; \
	if [ "$$MODE" != "dev" ] && [ "$$MODE" != "prod" ]; then \
		@$(call errorLog, Invalid MODE '$$MODE'. Use 'dev' or 'prod'.); \
		exit 1; \
	fi; \
	@$(call log, Starting middleware services in $$MODE mode...); \
	cd docker/middleware && bash run.sh $$MODE

.PHONY: env-stop
env-stop: ## Stop the environment services. Usage: make env-stop MODE=dev/prod (default: dev)
	@$(call LOG_TARGET)
	@MODE=$${MODE:-dev}; \
	if [ "$$MODE" != "dev" ] && [ "$$MODE" != "prod" ]; then \
		@$(call errorLog, Invalid MODE '$$MODE'. Use 'dev' or 'prod'.); \
		exit 1; \
	fi; \
	@$(call log, Stopping middleware services in $$MODE mode...); \
	cd docker/middleware && bash stop.sh $$MODE

.PHONY: env-clean
env-clean: ## Clean all middleware data. Usage: make env-clean MODE=dev/prod (default: dev)
	@$(call LOG_TARGET)
	@MODE=$${MODE:-dev}; \
	if [ "$$MODE" != "dev" ] && [ "$$MODE" != "prod" ]; then \
		@$(call errorLog, Invalid MODE '$$MODE'. Use 'dev' or 'prod'.); \
		exit 1; \
	fi; \
	@$(call log, Cleaning middleware data in $$MODE mode...); \
	read -p "This will delete all data! Are you sure? (yes/no): " confirm; \
	if [ "$$confirm" = "yes" ]; then \
		cd docker/middleware && docker compose -f docker-compose-$$MODE.yaml down -v; \
		@$(call log, Data cleaned successfully); \
	else \
		@$(call errorLog, Cleanup cancelled); \
	fi

##@ Local Run mode

.PHONY: frontend-install
frontend-install: ## Install frontend dependencies and build required packages.
	@$(call LOG_TARGET)
	@$(call log, Installing frontend dependencies...)
	@# Use --ignore-scripts to avoid husky and other prepare script errors during initial install
	cd frontend && npm install --ignore-scripts
	@$(call log, Fixing binary permissions for build tools...)
	@# Fix permissions for tailwindcss binary (required for spark-flow CSS build)
	@if [ -f "frontend/node_modules/tailwindcss/lib/cli.js" ]; then \
		chmod +x frontend/node_modules/tailwindcss/lib/cli.js; \
	fi
	@$(call log, Building spark-flow package...)
	@# Build spark-flow first as it's a dependency of the main package
	cd frontend/packages/spark-flow && npm run build
	@$(call log, Reinstalling cross-env to ensure proper installation...)
	@# Force reinstall cross-env to ensure dist files are properly generated
	cd frontend && npm install cross-env --force
	@$(call log, Fixing cross-env binary permissions...)
	@# Fix permissions for cross-env binary (required for dev server)
	@if [ -f "frontend/node_modules/cross-env/dist/bin/cross-env.js" ]; then \
		chmod +x frontend/node_modules/cross-env/dist/bin/cross-env.js; \
	fi
	@$(call log, Frontend dependencies installed successfully)

.PHONY: frontend-start
frontend-start: ## Start the frontend application in local development mode.
	@$(call LOG_TARGET)
	@if [ ! -d "frontend/node_modules" ]; then \
		$(call log, Dependencies not found. Running frontend-install...); \
		$(MAKE) frontend-install; \
	fi
	@$(call log, Starting frontend development server...)
	cd frontend/packages/main && npm run dev

.PHONY: backend-start
backend-start: ## Start the backend application in local development mode.
	@$(call LOG_TARGET)
	@$(call log, Starting backend server...)
	cd spring-ai-alibaba-admin-server-start && mvn spring-boot:run

.PHONY: local-all-start
local-all-start: ## Setup and show instructions for local development.
	@$(call LOG_TARGET)
	@echo ""
	@echo -e "\033[1;36m========================================\033[0m"
	@echo -e "\033[1;36m   Local Development Setup Guide\033[0m"
	@echo -e "\033[1;36m========================================\033[0m"
	@echo ""
	@echo -e "\033[1mStep 1: Start Middleware Services\033[0m"
	@echo -e "  Terminal 1: \033[32mmake env-start MODE=dev\033[0m"
	@echo ""
	@echo -e "\033[1mStep 2: Start Backend Server\033[0m"
	@echo -e "  Terminal 2: \033[32mmake backend-start\033[0m"
	@echo ""
	@echo -e "\033[1mStep 3: Start Frontend Dev Server\033[0m"
	@echo -e "  Terminal 3: \033[32mmake frontend-start\033[0m"
	@echo ""
	@echo -e "\033[1mQuick Start (if dependencies already installed):\033[0m"
	@echo -e "  \033[33mmake env-start MODE=dev && make backend-start & make frontend-start\033[0m"
	@echo ""
	@echo -e "\033[1mFirst Time Setup:\033[0m"
	@echo -e "  \033[33mmake frontend-install\033[0m  # Install and build frontend dependencies"
	@echo ""
	@echo -e "\033[1mAccess URLs:\033[0m"
	@echo -e "  Frontend: \033[36mhttp://localhost:8000\033[0m"
	@echo -e "  Backend:  \033[36mhttp://localhost:8080\033[0m"
	@echo ""
	@echo -e "\033[1;36m========================================\033[0m"
	@echo ""

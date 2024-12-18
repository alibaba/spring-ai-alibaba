APP_NAME := spring-ai-alibaba-cli
SRC := ./...
BUILD_DIR := build
ifeq ($(OS),Windows_NT)
    BIN := $(BUILD_DIR)/$(APP_NAME).exe
else
    BIN := $(BUILD_DIR)/$(APP_NAME)
endif
VERSION := $(shell git describe --tags --always)
STATIC_DIR := pkg/static/dist
TEMPLATE_DIR := pkg/static/templates
TEMPLATE_EXAMPLE := helloworld-example
TEMPLATE_PATH := templates/$(TEMPLATE_EXAMPLE)

LDFLAGS := -ldflags="-X 'github.com/alibaba/spring-ai-alibaba/pkg/constant.Version=$(VERSION)' -X 'github.com/alibaba/spring-ai-alibaba/pkg/constant.TemplatePath=$(TEMPLATE_PATH)'"

.DEFAULT_GOAL := build

.PHONY: deps
deps:
	go mod tidy
	go mod download

.PHONY: build
build: deps build-frontend cp-templates
	mkdir -p $(BUILD_DIR)
	go build $(LDFLAGS) -o $(BIN) .
	@echo "Build success"

.PHONY: clean
clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(STATIC_DIR)
	rm -rf $(TEMPLATE_DIR)

.PHONY: test
test:
	go test -v -tags=test $(SRC)

.PHONY: lint
lint:
	golangci-lint run

.PHONY: fmt
fmt:
	go fmt $(SRC)

.PHONY: install
install:
	go install $(LDFLAGS) .

.PHONY: run
run:
	go run $(LDFLAGS) .

.PHONY: build-frontend
build-frontend:
	cd ../ui && npm run build
	rm -rf $(STATIC_DIR)
	mv ../ui/build $(STATIC_DIR)

.PHONY: cp-templates
cp-templates:
	mkdir -p $(TEMPLATE_DIR)
	cp -r ../../spring-ai-alibaba-examples/$(TEMPLATE_EXAMPLE)/ $(TEMPLATE_DIR)

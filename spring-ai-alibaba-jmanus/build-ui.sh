# /*
#  * Copyright 2025 the original author or authors.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      https://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#  */
#!/bin/bash

# Build frontend UI and deploy to Spring Boot static resources directory
# For macOS

set -e  # Exit immediately on error

# Get the absolute path of the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Define color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Output colored logs
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if necessary commands exist
check_commands() {
    log_info "Checking necessary commands..."

    if ! command -v pnpm &> /dev/null; then
        log_error "pnpm is not installed, please install pnpm first"
        log_info "Installation command: npm install -g pnpm"
        exit 1
    fi

    log_success "Command check completed"
}

# Check directory structure
check_directories() {
    log_info "Checking project directory structure..."

    if [ ! -d "$PROJECT_ROOT/ui-vue3" ]; then
        log_error "ui-vue3 directory does not exist: $PROJECT_ROOT/ui-vue3"
        exit 1
    fi

    if [ ! -f "$PROJECT_ROOT/ui-vue3/package.json" ]; then
        log_error "ui-vue3/package.json file does not exist"
        exit 1
    fi

    if [ ! -d "$PROJECT_ROOT/src/main/resources" ]; then
        log_error "Spring Boot resources directory does not exist: $PROJECT_ROOT/src/main/resources"
        exit 1
    fi

    log_success "Directory structure check completed"
}

# Build frontend project
build_frontend() {
    log_info "Starting frontend project build..."

    cd "$PROJECT_ROOT/ui-vue3"

    # Check if node_modules exists, install dependencies if not
    if [ ! -d "node_modules" ]; then
        log_warning "node_modules does not exist, installing dependencies..."
        pnpm install
    fi

    # Run build command
    log_info "Running pnpm run build..."
    pnpm run build

    # Check build result
    if [ ! -d "ui" ]; then
        log_error "Build failed, ui directory does not exist"
        exit 1
    fi

    log_success "Frontend project build completed"
}

# Clear static resources directory
clean_static_directory() {
    log_info "Clearing static resources directory..."

    STATIC_DIR="$PROJECT_ROOT/src/main/resources/static"

    # Create directory (if it doesn't exist)
    mkdir -p "$STATIC_DIR"

    # Clear directory contents
    if [ "$(ls -A $STATIC_DIR)" ]; then
        log_warning "Deleting existing files in $STATIC_DIR..."
        rm -rf "$STATIC_DIR"/*
        log_success "Static resources directory cleared"
    else
        log_info "Static resources directory is already empty"
    fi
}

# Copy build files
copy_build_files() {
    log_info "Copying build files to static resources directory..."

    SOURCE_DIR="$PROJECT_ROOT/ui-vue3/ui"
    TARGET_DIR="$PROJECT_ROOT/src/main/resources/static/"

    if [ ! -d "$SOURCE_DIR" ]; then
        log_error "Source directory does not exist: $SOURCE_DIR"
        exit 1
    fi

    # Copy files
    cp -r "$SOURCE_DIR" "$TARGET_DIR/"

    # Verify copy result
    if [ "$(ls -A $TARGET_DIR)" ]; then
        log_success "File copy completed"
        log_info "Copied files:"
        ls -la "$TARGET_DIR"
    else
        log_error "File copy failed, target directory is empty"
        exit 1
    fi
}

# Show build summary
show_summary() {
    log_success "=== Build Completed ==="
    log_info "Frontend files successfully deployed to: $PROJECT_ROOT/src/main/resources/static/"
    log_info "You can now run the Spring Boot application"
    log_info ""
    log_info "Example startup commands:"
    log_info "  mvn spring-boot:run"
    log_info "  or"
    log_info "  java -jar target/spring-ai-alibaba-jmanus-*.jar"
}

# Main function
main() {
    log_info "Starting frontend build and deployment process..."
    log_info "Project root directory: $PROJECT_ROOT"
    echo ""

    check_commands
    check_directories
    build_frontend
    clean_static_directory
    copy_build_files
    show_summary

    log_success "All steps completed!"
}

# Error handling
trap 'log_error "Error occurred during script execution, exit code: $?"' ERR

# Run main function
main "$@"

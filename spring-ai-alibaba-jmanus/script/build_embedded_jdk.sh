#!/bin/bash
/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


# Build JManus with embedded JDK 21 for macOS
# This script downloads JDK 21 and packages it with the Spring Boot jar

set -e

# Configuration
APP_NAME="JManus"
VERSION="3.0.0-SNAPSHOT"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# JDK download URLs - using Amazon Corretto 21
JDK_URL_MACOS_X64="https://corretto.aws/downloads/latest/amazon-corretto-21-x64-macos-jdk.tar.gz"
JDK_URL_MACOS_AARCH64="https://corretto.aws/downloads/latest/amazon-corretto-21-aarch64-macos-jdk.tar.gz"

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

# Detect macOS architecture
detect_architecture() {
    local arch=$(uname -m)
    case $arch in
        "x86_64")
            echo "macos-x64"
            ;;
        "arm64")
            echo "macos-aarch64"
            ;;
        *)
            log_error "Unsupported architecture: $arch"
            exit 1
            ;;
    esac
}

# Download and extract JDK
download_jdk() {
    local arch=$1
    local jdk_url=""

    case $arch in
        "macos-x64")
            jdk_url="$JDK_URL_MACOS_X64"
            ;;
        "macos-aarch64")
            jdk_url="$JDK_URL_MACOS_AARCH64"
            ;;
        *)
            log_error "Unsupported architecture: $arch"
            exit 1
            ;;
    esac

    local jdk_dir="$PROJECT_ROOT/dist/jdk"
    local temp_dir="$PROJECT_ROOT/dist/temp"

    log_info "Downloading JDK 21 for $arch..."
    log_info "URL: $jdk_url"

    # Create directories
    mkdir -p "$temp_dir"
    rm -rf "$jdk_dir"
    mkdir -p "$jdk_dir"

    # Download JDK
    local jdk_archive="$temp_dir/amazon-corretto-21.tar.gz"
    curl -L -o "$jdk_archive" "$jdk_url"

    if [ ! -f "$jdk_archive" ]; then
        log_error "Failed to download JDK"
        exit 1
    fi

    log_info "Extracting JDK..."
    tar -xzf "$jdk_archive" -C "$temp_dir"

    # Find the extracted JDK directory
    # Amazon Corretto typically extracts to amazon-corretto-21.jdk or similar
    local extracted_jdk=""

    # Try different naming patterns
    for pattern in "amazon-corretto-*.jdk" "amazon-corretto-*" "corretto-*.jdk" "corretto-*" "jdk*"; do
        extracted_jdk=$(find "$temp_dir" -name "$pattern" -type d | head -1)
        if [ -n "$extracted_jdk" ]; then
            break
        fi
    done

    if [ -z "$extracted_jdk" ]; then
        log_error "Could not find extracted JDK directory"
        log_info "Contents of temp directory:"
        ls -la "$temp_dir"
        exit 1
    fi

    log_info "Found JDK directory: $(basename "$extracted_jdk")"

    # Move JDK to final location
    mv "$extracted_jdk" "$jdk_dir/jdk-21"

    # Clean up
    rm -rf "$temp_dir"

    log_success "JDK 21 downloaded and extracted to $jdk_dir/jdk-21"
}

# Build Spring Boot jar and extract it
build_and_extract_jar() {
    log_info "Building Spring Boot jar..."

    cd "$PROJECT_ROOT"

    # Build the project
    cd "$PROJECT_ROOT/.."
    ./mvnw -f spring-ai-alibaba-jmanus/pom.xml clean package -DskipTests
    cd "$PROJECT_ROOT"

    # Check if jar was built
    local jar_file=$(find "$PROJECT_ROOT/target" -name "spring-ai-alibaba-jmanus-*.jar" | head -1)
    if [ ! -f "$jar_file" ]; then
        log_error "Failed to build jar file"
        exit 1
    fi

    log_success "Jar built: $(basename "$jar_file")"

    # Extract jar to a temporary directory
    local extract_dir="$PROJECT_ROOT/target/app"
    rm -rf "$extract_dir"
    mkdir -p "$extract_dir"

    log_info "Extracting jar file for exploded startup..."
    cd "$extract_dir"
    unzip -q "$jar_file"

    if [ ! -d "BOOT-INF" ]; then
        log_error "Failed to extract jar file properly"
        exit 1
    fi

    log_success "Jar extracted successfully"
}

# Create distribution package
create_distribution() {
    local arch=$1
    local extract_dir=$2
    local dist_dir="$PROJECT_ROOT/dist/$arch"

    log_info "Creating distribution package for $arch..."

    # Create distribution directory structure
    rm -rf "$dist_dir"
    mkdir -p "$dist_dir/bin"
    mkdir -p "$dist_dir/app"
    mkdir -p "$dist_dir/docs"

    # Copy JDK
    cp -r "$PROJECT_ROOT/dist/jdk/jdk-21" "$dist_dir/jdk/"

    # Copy extracted jar contents
    log_info "Copying extracted application files..."
    (cd "$extract_dir" && cp -r . "$dist_dir/app/")

    # Copy scripts
    cp "$SCRIPT_DIR/start_jmanus.sh" "$dist_dir/bin/"
    chmod +x "$dist_dir/bin/start_jmanus.sh"

    # Copy documentation
    cp "$PROJECT_ROOT/README"*.md "$dist_dir/docs/" 2>/dev/null || true

    # Create build info
    cat > "$dist_dir/BUILD_INFO.txt" << EOF
Build completed on $arch at $(date)
Version: $VERSION
Platform: $arch
JDK Version: 21.0.2
Build Type: Embedded JDK Distribution (Exploded JAR)
Commit: $(git rev-parse HEAD 2>/dev/null || echo "unknown")
EOF

    log_success "Distribution package created at $dist_dir"
}

# Create DMG package
create_dmg() {
    local arch=$1
    local dist_dir="$PROJECT_ROOT/dist/$arch"

    log_info "Creating DMG package for $arch..."

    if [ ! -f "$SCRIPT_DIR/create_dmg_embedded.sh" ]; then
        log_warning "DMG creation script not found, skipping DMG creation"
        return
    fi

    chmod +x "$SCRIPT_DIR/create_dmg_embedded.sh"
    "$SCRIPT_DIR/create_dmg_embedded.sh" --platform "$arch" --version "$VERSION"
}

# Main function
main() {
    log_info "Starting JManus build with embedded JDK 21..."
    log_info "Project root: $PROJECT_ROOT"

    # Parse arguments
    local target_arch=""
    while [[ $# -gt 0 ]]; do
        case $1 in
            --arch)
                target_arch="$2"
                shift 2
                ;;
            --version)
                VERSION="$2"
                shift 2
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Usage: $0 [--arch <macos-x64|macos-aarch64>] [--version <version>]"
                exit 1
                ;;
        esac
    done

    # Auto-detect architecture if not specified
    if [ -z "$target_arch" ]; then
        target_arch=$(detect_architecture)
        log_info "Auto-detected architecture: $target_arch"
    fi

    # Validate architecture
    if [[ ! "$target_arch" =~ ^macos-(x64|aarch64)$ ]]; then
        log_error "Invalid architecture: $target_arch"
        log_error "Supported architectures: macos-x64, macos-aarch64"
        exit 1
    fi

    # Create dist directory
    mkdir -p "$PROJECT_ROOT/dist"

    # Download JDK
    download_jdk "$target_arch"

    # Build and extract jar
    build_and_extract_jar
    local extract_dir="$PROJECT_ROOT/target/app"

    # Create distribution package
    create_distribution "$target_arch" "$extract_dir"

    # Create DMG if possible
    create_dmg "$target_arch"

    log_success "Build completed successfully!"
    log_info "Distribution package: $PROJECT_ROOT/dist/$target_arch"
    log_info ""
    log_info "To run JManus:"
    log_info "  cd $PROJECT_ROOT/dist/$target_arch"
    log_info "  ./bin/start_jmanus.sh"
}

# Error handling
trap 'log_error "Error occurred during build process, exit code: $?"' ERR

# Run main function
main "$@"

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


# DMG creation script for JManus with embedded JDK on macOS
# This script creates a distributable DMG package with embedded JDK

set -e

# Cleanup function for script interruption
cleanup_on_exit() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo "Script interrupted, cleaning up..."

        # More comprehensive cleanup for Intel Macs
        echo "Performing comprehensive cleanup..."

        # Get all disk image info
        local dmg_info=$(hdiutil info 2>/dev/null || true)

        # Unmount any JManus related mounts
        echo "$dmg_info" | grep -E "(JManus|temp.*JManus)" | awk '{print $1}' | while read -r device; do
            if [ -n "$device" ]; then
                echo "Force unmounting $device"
                hdiutil detach "$device" -force 2>/dev/null || true
            fi
        done

        # Clean up project directory DMG files
        echo "$dmg_info" | grep -E "$PROJECT_ROOT" | awk '{print $1}' | while read -r device; do
            if [ -n "$device" ]; then
                echo "Force unmounting project DMG: $device"
                hdiutil detach "$device" -force 2>/dev/null || true
            fi
        done

        # Remove temporary files
        rm -f "$PROJECT_ROOT/dist/temp-"*".dmg" 2>/dev/null || true
        rm -f "$PROJECT_ROOT/dist/"*".dmg.sparseimage" 2>/dev/null || true
        rm -f "$PROJECT_ROOT/dist/"*".tmp" 2>/dev/null || true

        # Force sync
        sync
        sleep 2
    fi
}

# Set up cleanup on script exit
trap cleanup_on_exit EXIT

# Configuration
APP_NAME="JManus"
APP_BUNDLE_NAME="JManus.app"
DMG_NAME="JManus-Installer"
TEMP_DMG_NAME="temp-${DMG_NAME}"
VOLUME_NAME="JManus Installer"

# Get script directory and project root early
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Parse arguments
PLATFORM=""
VERSION="3.0.0-SNAPSHOT"

while [[ $# -gt 0 ]]; do
    case $1 in
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [ -z "$PLATFORM" ]; then
    echo "Usage: $0 --platform <platform> [--version <version>]"
    echo "Example: $0 --platform macos-aarch64 --version 3.0.0"
    exit 1
fi

echo "Creating DMG for platform: $PLATFORM"
echo "Version: $VERSION"

# Set distribution directory
DIST_DIR="$PROJECT_ROOT/dist/$PLATFORM"

# Check if distribution exists
if [ ! -d "$DIST_DIR" ]; then
    echo "Error: Distribution directory not found at $DIST_DIR"
    echo "Please run build_embedded_jdk.sh first"
    exit 1
fi

# Create temporary directory structure
TEMP_DIR=$(mktemp -d)
APP_BUNDLE_DIR="$TEMP_DIR/$APP_BUNDLE_NAME"
APP_CONTENTS_DIR="$APP_BUNDLE_DIR/Contents"
APP_MACOS_DIR="$APP_CONTENTS_DIR/MacOS"
APP_RESOURCES_DIR="$APP_CONTENTS_DIR/Resources"

mkdir -p "$APP_MACOS_DIR"
mkdir -p "$APP_RESOURCES_DIR"

# Copy the entire distribution to the app bundle
cp -r "$DIST_DIR"/* "$APP_RESOURCES_DIR/"

# Create main executable wrapper script
cat > "$APP_MACOS_DIR/JManus" << 'EOF'
#!/bin/bash
# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_RESOURCES="$SCRIPT_DIR/../Resources"

# Change to resources directory
cd "$APP_RESOURCES"

# Export API key if set
if [ -n "$DASHSCOPE_API_KEY" ]; then
    export DASHSCOPE_API_KEY
fi

# Launch the application using the embedded startup script
exec "$APP_RESOURCES/bin/start_jmanus.sh" --terminal
EOF

chmod +x "$APP_MACOS_DIR/JManus"

# Create Info.plist for the app bundle
cat > "$APP_CONTENTS_DIR/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>JManus</string>
    <key>CFBundleIdentifier</key>
    <string>com.alibaba.spring-ai.jmanus</string>
    <key>CFBundleName</key>
    <string>JManus</string>
    <key>CFBundleDisplayName</key>
    <string>JManus</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.developer-tools</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSRequiresAquaSystemAppearance</key>
    <false/>
    <key>LSEnvironment</key>
    <dict>
        <key>LC_CTYPE</key>
        <string>UTF-8</string>
    </dict>
</dict>
</plist>
EOF

# Create PkgInfo
echo "APPLNONE" > "$APP_CONTENTS_DIR/PkgInfo"

# Create the DMG staging area
DMG_STAGING_DIR="$TEMP_DIR/dmg"
mkdir -p "$DMG_STAGING_DIR"

# Copy app bundle to staging area
cp -r "$APP_BUNDLE_DIR" "$DMG_STAGING_DIR/"

# Create a symlink to Applications folder
ln -s /Applications "$DMG_STAGING_DIR/Applications"

# Add additional files
mkdir -p "$DMG_STAGING_DIR/Documentation"
cp "$DIST_DIR/docs"/*.md "$DMG_STAGING_DIR/Documentation/" 2>/dev/null || true
cp "$DIST_DIR/BUILD_INFO.txt" "$DMG_STAGING_DIR/" 2>/dev/null || true

# Calculate DMG size (add 50MB buffer)
SIZE_MB=$(du -sm "$DMG_STAGING_DIR" | cut -f1)
SIZE_MB=$((SIZE_MB + 50))

# Create the DMG
DMG_PATH="$PROJECT_ROOT/dist/${DMG_NAME}-${VERSION}-${PLATFORM}.dmg"
TEMP_DMG_PATH="$PROJECT_ROOT/dist/${TEMP_DMG_NAME}.dmg"

echo "Creating DMG (${SIZE_MB}MB)..."

# Remove existing DMG files
rm -f "$DMG_PATH" "$TEMP_DMG_PATH"

# Function to unmount any existing DMG mounts
unmount_existing_dmgs() {
    echo "Checking for existing DMG mounts..."

    # First, get all disk images info
    local dmg_info=$(hdiutil info 2>/dev/null || true)

    # Check for any JManus related mounts by volume name
    echo "$dmg_info" | grep -E "/Volumes/.*JManus" | while read -r line; do
        local device=$(echo "$line" | awk '{print $1}')
        if [ -n "$device" ]; then
            echo "Unmounting JManus volume: $device"
            hdiutil detach "$device" -force 2>/dev/null || true
        fi
    done

    # Check for any temp DMG files that might be mounted
    echo "$dmg_info" | grep -E "temp.*JManus.*Installer" | while read -r line; do
        local device=$(echo "$line" | awk '{print $1}')
        if [ -n "$device" ]; then
            echo "Unmounting temp DMG: $device"
            hdiutil detach "$device" -force 2>/dev/null || true
        fi
    done

    # Check for any DMG files from our project directory that might be mounted
    echo "$dmg_info" | grep -E "$PROJECT_ROOT" | while read -r line; do
        local device=$(echo "$line" | awk '{print $1}')
        if [ -n "$device" ]; then
            echo "Unmounting project DMG: $device"
            hdiutil detach "$device" -force 2>/dev/null || true
        fi
    done

    # Additional cleanup - unmount any disk images that might be related
    local mounted_volumes=$(mount | grep -E "JManus|temp.*dmg" | awk '{print $1}' || true)
    if [ -n "$mounted_volumes" ]; then
        echo "Found additional mounted volumes to clean up..."
        echo "$mounted_volumes" | while read -r device; do
            if [ -n "$device" ]; then
                echo "Unmounting volume: $device"
                hdiutil detach "$device" -force 2>/dev/null || true
            fi
        done
    fi

    # Wait longer for system to clean up on older macOS versions
    echo "Waiting for system cleanup..."
    sleep 5

    # Force sync to ensure all disk operations are complete
    sync
}

# Unmount any existing DMG mounts
unmount_existing_dmgs

# Additional cleanup for Intel Macs - remove any leftover files
echo "Cleaning up any leftover DMG files..."
rm -f "$PROJECT_ROOT/dist/"*".dmg.sparseimage" 2>/dev/null || true
rm -f "$PROJECT_ROOT/dist/"*".tmp" 2>/dev/null || true

# Create temporary DMG with additional error handling
echo "Creating temporary DMG..."

# Try to create the DMG with retry logic
for create_attempt in 1 2 3; do
    echo "DMG creation attempt $create_attempt..."

    if [ $create_attempt -gt 1 ]; then
        echo "Retrying DMG creation after cleanup..."
        unmount_existing_dmgs
        rm -f "$TEMP_DMG_PATH" 2>/dev/null || true
        sleep 3
    fi

    if hdiutil create -srcfolder "$DMG_STAGING_DIR" \
        -volname "$VOLUME_NAME" \
        -fs HFS+ \
        -format UDRW \
        -size "${SIZE_MB}m" \
        "$TEMP_DMG_PATH" 2>/dev/null; then
        echo "DMG creation successful on attempt $create_attempt"
        break
    else
        echo "DMG creation failed on attempt $create_attempt"
        if [ $create_attempt -eq 3 ]; then
            echo "Failed to create DMG after 3 attempts"
            exit 1
        fi
        echo "Waiting before retry..."
        sleep 5
    fi
done

# Verify DMG was created successfully
if [ ! -f "$TEMP_DMG_PATH" ]; then
    echo "Error: Failed to create temporary DMG"
    exit 1
fi

echo "Temporary DMG created successfully: $TEMP_DMG_PATH"

# Mount the temporary DMG
echo "Mounting temporary DMG for configuration..."
MOUNT_OUTPUT=$(hdiutil attach -readwrite -noverify -noautoopen "$TEMP_DMG_PATH" 2>&1)
MOUNT_RESULT=$?

if [ $MOUNT_RESULT -ne 0 ]; then
    echo "Error: Failed to mount temporary DMG"
    echo "Mount output: $MOUNT_OUTPUT"
    rm -f "$TEMP_DMG_PATH"
    exit 1
fi

MOUNT_POINT=$(echo "$MOUNT_OUTPUT" | grep -E '/Volumes/' | tail -1)
MOUNT_DEVICE=$(echo "$MOUNT_OUTPUT" | grep -E '^/dev/disk[0-9]+s[0-9]+' | awk '{print $1}')

if [ -z "$MOUNT_POINT" ] || [ -z "$MOUNT_DEVICE" ]; then
    echo "Error: Could not determine mount point or device"
    echo "Mount output: $MOUNT_OUTPUT"
    rm -f "$TEMP_DMG_PATH"
    exit 1
fi

echo "DMG mounted at: $MOUNT_POINT (device: $MOUNT_DEVICE)"

# Set DMG window properties and background
if [ -n "$MOUNT_POINT" ]; then
    echo "Configuring DMG appearance..."

    # Create .DS_Store for window settings
    echo '
    on run argv
        tell application "Finder"
            tell disk "'$VOLUME_NAME'"
                open
                set current view of container window to icon view
                set toolbar visible of container window to false
                set statusbar visible of container window to false
                set the bounds of container window to {100, 100, 600, 400}
                set viewOptions to the icon view options of container window
                set arrangement of viewOptions to not arranged
                set icon size of viewOptions to 72
                set position of item "JManus.app" of container window to {150, 120}
                set position of item "Applications" of container window to {350, 120}
                if exists item "Documentation" then
                    set position of item "Documentation" of container window to {400, 220}
                end if
                update without registering applications
                delay 2
            end tell
        end tell
    end run
    ' | osascript

    # Give the system time to write the .DS_Store file
    sleep 3

    # Safely unmount with retry
    echo "Unmounting temporary DMG..."
    for i in 1 2 3; do
        if hdiutil detach "$MOUNT_DEVICE" 2>/dev/null; then
            echo "Successfully unmounted on attempt $i"
            break
        else
            echo "Unmount attempt $i failed, retrying..."
            sleep 2
            if [ $i -eq 3 ]; then
                echo "Force unmounting..."
                hdiutil detach "$MOUNT_DEVICE" -force || true
            fi
        fi
    done
else
    echo "Warning: Mount point not available, skipping DMG appearance configuration"
fi

# Convert to compressed read-only DMG
echo "Compressing DMG..."

# Function to cleanup on conversion failure
cleanup_on_failure() {
    echo "Cleaning up after conversion failure..."

    # Unmount any mounts that might still be active
    unmount_existing_dmgs

    # Remove temp DMG file
    rm -f "$TEMP_DMG_PATH"
}

# Add retry logic for hdiutil convert with proper cleanup
for attempt in 1 2 3; do
    echo "DMG compression attempt $attempt..."

    # Ensure clean state before each attempt
    if [ $attempt -gt 1 ]; then
        cleanup_on_failure
        sleep 5
    fi

    if hdiutil convert "$TEMP_DMG_PATH" \
        -format UDZO \
        -imagekey zlib-level=9 \
        -o "$DMG_PATH" 2>/dev/null; then
        echo "DMG compression successful on attempt $attempt"
        break
    else
        echo "DMG compression failed on attempt $attempt"
        if [ $attempt -eq 3 ]; then
            echo "Failed to compress DMG after 3 attempts"
            cleanup_on_failure
            exit 1
        fi
        echo "Waiting 5 seconds before retry..."
        sleep 5
    fi
done

# Clean up
rm -f "$TEMP_DMG_PATH"
rm -rf "$TEMP_DIR"

# Verify the DMG
if [ -f "$DMG_PATH" ]; then
    DMG_SIZE=$(ls -lh "$DMG_PATH" | awk '{print $5}')
    echo "DMG created successfully: $DMG_PATH ($DMG_SIZE)"

    # Test mount the DMG to verify it works
    echo "Verifying DMG..."
    TEST_MOUNT_OUTPUT=$(hdiutil attach -readonly -noverify -noautoopen "$DMG_PATH" 2>&1)
    TEST_MOUNT_RESULT=$?

    if [ $TEST_MOUNT_RESULT -eq 0 ]; then
        TEST_MOUNT_DEVICE=$(echo "$TEST_MOUNT_OUTPUT" | grep -E '^/dev/disk[0-9]+s[0-9]+' | awk '{print $1}')
        if [ -n "$TEST_MOUNT_DEVICE" ]; then
            echo "DMG verification successful"
            hdiutil detach "$TEST_MOUNT_DEVICE" >/dev/null 2>&1
        else
            echo "Warning: Could not determine test mount device"
        fi
    else
        echo "Warning: DMG verification failed"
        echo "Verification output: $TEST_MOUNT_OUTPUT"
    fi
else
    echo "Error: Failed to create DMG"
    exit 1
fi

echo "DMG creation completed: $DMG_PATH"

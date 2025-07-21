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


# DMG creation script for JManus on macOS
# This script creates a distributable DMG package

set -e

# Configuration
APP_NAME="JManus"
APP_BUNDLE_NAME="JManus.app"
DMG_NAME="JManus-Installer"
TEMP_DMG_NAME="temp-${DMG_NAME}"
VOLUME_NAME="JManus Installer"
APP_SIZE_MB=100  # Estimated size, adjust as needed

# Parse arguments
PLATFORM=""
EXECUTABLE_NAME=""
VERSION="3.0.0-SNAPSHOT"

while [[ $# -gt 0 ]]; do
    case $1 in
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        --executable)
            EXECUTABLE_NAME="$2"
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

if [ -z "$PLATFORM" ] || [ -z "$EXECUTABLE_NAME" ]; then
    echo "Usage: $0 --platform <platform> --executable <executable_name> [--version <version>]"
    echo "Example: $0 --platform macos-arm --executable spring-ai-alibaba-jmanus --version 3.0.0"
    exit 1
fi

echo "Creating DMG for platform: $PLATFORM"
echo "Executable: $EXECUTABLE_NAME"
echo "Version: $VERSION"

# Create temporary directory structure
TEMP_DIR=$(mktemp -d)
APP_BUNDLE_DIR="$TEMP_DIR/$APP_BUNDLE_NAME"
APP_CONTENTS_DIR="$APP_BUNDLE_DIR/Contents"
APP_MACOS_DIR="$APP_CONTENTS_DIR/MacOS"
APP_RESOURCES_DIR="$APP_CONTENTS_DIR/Resources"

mkdir -p "$APP_MACOS_DIR"
mkdir -p "$APP_RESOURCES_DIR/bin"
mkdir -p "$APP_RESOURCES_DIR/docs"

# Copy the executable
if [ ! -f "dist/$PLATFORM/$EXECUTABLE_NAME" ]; then
    echo "Error: Executable not found at dist/$PLATFORM/$EXECUTABLE_NAME"
    exit 1
fi

cp "dist/$PLATFORM/$EXECUTABLE_NAME" "$APP_RESOURCES_DIR/bin/"
chmod +x "$APP_RESOURCES_DIR/bin/$EXECUTABLE_NAME"

# Copy startup script
cp "script/start_jmanus.sh" "$APP_RESOURCES_DIR/"
chmod +x "$APP_RESOURCES_DIR/start_jmanus.sh"

# Copy documentation
cp "dist/$PLATFORM/README"*.md "$APP_RESOURCES_DIR/docs/" 2>/dev/null || true
cp "dist/$PLATFORM/BUILD_INFO.txt" "$APP_RESOURCES_DIR/docs/" 2>/dev/null || true
cp "INSTALL-macOS.md" "$APP_RESOURCES_DIR/docs/" 2>/dev/null || true

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
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>LSUIElement</key>
    <false/>
</dict>
</plist>
EOF

# Create the main executable launcher
cat > "$APP_MACOS_DIR/JManus" << 'EOF'
#!/bin/bash

# Debug log function
debug_log() {
    echo "$(date): $1" >> /tmp/jmanus_debug.log
}

debug_log "JManus launcher started"

# Get the directory of this script (MacOS directory)
MACOS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESOURCES_DIR="$(dirname "$MACOS_DIR")/Resources"

debug_log "MACOS_DIR: $MACOS_DIR"
debug_log "RESOURCES_DIR: $RESOURCES_DIR"

# Check if resources directory exists
if [ ! -d "$RESOURCES_DIR" ]; then
    debug_log "Error: Resources directory not found at $RESOURCES_DIR"
    osascript -e 'display alert "JManus Error" message "Application resources not found. Please reinstall JManus." as critical'
    exit 1
fi

# Check if startup script exists
if [ ! -f "$RESOURCES_DIR/start_jmanus.sh" ]; then
    debug_log "Error: start_jmanus.sh not found at $RESOURCES_DIR/start_jmanus.sh"
    osascript -e 'display alert "JManus Error" message "Startup script not found. Please reinstall JManus." as critical'
    exit 1
fi

debug_log "Changing to Resources directory and executing startup script"

# Change to Resources directory and run the startup script
cd "$RESOURCES_DIR"
debug_log "Current directory: $(pwd)"

# Make sure the startup script is executable
chmod +x "./start_jmanus.sh"

# Execute the startup script
debug_log "Executing start_jmanus.sh"
exec ./start_jmanus.sh
EOF
chmod +x "$APP_MACOS_DIR/JManus"

# Create application info file in Resources
cat > "$APP_RESOURCES_DIR/Application Info.txt" << EOF
JManus Application
Version: $VERSION
Platform: $PLATFORM
Build Date: $(date)

To start the application:
- Double-click the JManus.app icon

Requirements:
- macOS 10.15 or later
- DashScope API Key (you will be prompted on first run)

The application will be available at http://localhost:18080

For more information, see the documentation in the 'docs' folder.
EOF

# Create README for the DMG
cat > "$TEMP_DIR/README.txt" << EOF
JManus Application Installer

To install JManus:
1. Drag the "JManus.app" to your Applications folder or any desired location
2. Double-click JManus.app to start the application

You will be prompted to enter your DashScope API Key on first run.
The application will be available at http://localhost:18080

For support and documentation, visit:
https://github.com/alibaba/spring-ai-alibaba
EOF

# Calculate total size
TOTAL_SIZE_KB=$(du -sk "$TEMP_DIR" | cut -f1)
TOTAL_SIZE_MB=$((TOTAL_SIZE_KB / 1024 + 10))  # Add 10MB buffer

echo "Creating DMG with size: ${TOTAL_SIZE_MB}MB"

# Create DMG
DMG_PATH="dist/$PLATFORM/${DMG_NAME}-${VERSION}-${PLATFORM}.dmg"
TEMP_DMG_PATH="dist/$PLATFORM/${TEMP_DMG_NAME}.dmg"

# Remove existing DMG if it exists
rm -f "$DMG_PATH" "$TEMP_DMG_PATH"

# Create temporary DMG
hdiutil create -srcfolder "$TEMP_DIR" -volname "$VOLUME_NAME" -fs HFS+ -fsargs "-c c=64,a=16,e=16" -format UDRW -size ${TOTAL_SIZE_MB}m "$TEMP_DMG_PATH"

# Mount the temporary DMG
MOUNT_POINT="/Volumes/$VOLUME_NAME"
hdiutil attach "$TEMP_DMG_PATH" -readwrite -mount required

# Wait for mount
sleep 2

# Create Applications alias for easier installation
ln -sf "/Applications" "$MOUNT_POINT/Applications"

# Optional: Set DMG window properties using AppleScript (simplified)
osascript << EOF || true
tell application "Finder"
    tell disk "$VOLUME_NAME"
        open
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {400, 100, 800, 400}
        set viewOptions to the icon view options of container window
        set arrangement of viewOptions to not arranged
        set icon size of viewOptions to 72
        make new alias file at container window to POSIX file "/Applications" with properties {name:"Applications"}
        set position of item "JManus.app" of container window to {150, 200}
        set position of item "Applications" of container window to {350, 200}
        set position of item "README.txt" of container window to {250, 300}
        close
        open
        update without registering applications
        delay 2
    end tell
end tell
EOF

# Unmount the DMG
hdiutil detach "$MOUNT_POINT"

# Convert to compressed read-only DMG
hdiutil convert "$TEMP_DMG_PATH" -format UDZO -imagekey zlib-level=9 -o "$DMG_PATH"

# Clean up
rm -f "$TEMP_DMG_PATH"
rm -rf "$TEMP_DIR"

echo "DMG created successfully: $DMG_PATH"

# Show file size
if [ -f "$DMG_PATH" ]; then
    DMG_SIZE=$(du -h "$DMG_PATH" | cut -f1)
    echo "DMG size: $DMG_SIZE"
fi

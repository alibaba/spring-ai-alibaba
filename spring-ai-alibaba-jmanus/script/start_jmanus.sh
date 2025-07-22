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

# JManus macOS Launcher Script
# This script will prompt for DASHSCOPE_API_KEY and start the JManus application

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Determine the application root directory (parent of bin directory)
if [[ "$SCRIPT_DIR" == *"/bin" ]]; then
    # Running from distributed package structure
    APP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
    APP_DIR="$APP_ROOT/app"
    JDK_HOME="$APP_ROOT/jdk/jdk-21"
else
    # Running from source/development
    APP_ROOT="$SCRIPT_DIR"
    APP_DIR="$APP_ROOT/target/extracted"
    JDK_HOME=""
fi

# Check if we should use embedded JDK or system java
if [ -d "$JDK_HOME" ]; then
    # Use embedded JDK
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS JDK structure
        JAVA_EXECUTABLE="$JDK_HOME/Contents/Home/bin/java"
        if [ ! -f "$JAVA_EXECUTABLE" ]; then
            # Try alternative path structure
            JAVA_EXECUTABLE="$JDK_HOME/bin/java"
        fi
    else
        JAVA_EXECUTABLE="$JDK_HOME/bin/java"
    fi

    if [ ! -f "$JAVA_EXECUTABLE" ]; then
        echo "Error: Embedded JDK not found at expected location"
        echo "Looked for: $JAVA_EXECUTABLE"
        echo "Falling back to system java..."
        JAVA_EXECUTABLE="java"
    else
        echo "Using embedded JDK: $JAVA_EXECUTABLE"
    fi
else
    # Use system java
    JAVA_EXECUTABLE="java"
fi

# Check if exploded jar exists, fallback to legacy modes
if [ -d "$APP_DIR" ] && [ -d "$APP_DIR/BOOT-INF" ]; then
    # Exploded jar mode
    EXECUTION_MODE="exploded"
    MAIN_CLASS="com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication"

    # Build classpath for exploded jar
    CLASSPATH="$APP_DIR:$APP_DIR/BOOT-INF/classes"
    if [ -d "$APP_DIR/BOOT-INF/lib" ]; then
        for jar in "$APP_DIR/BOOT-INF/lib"/*.jar; do
            if [ -f "$jar" ]; then
                CLASSPATH="$CLASSPATH:$jar"
            fi
        done
    fi

    JMANUS_EXECUTABLE="$JAVA_EXECUTABLE"
    JMANUS_ARGS="-cp $CLASSPATH $MAIN_CLASS"
elif [ -f "$APP_ROOT/lib/spring-ai-alibaba-jmanus.jar" ]; then
    # Fat jar mode (fallback)
    JMANUS_EXECUTABLE="$JAVA_EXECUTABLE"
    JMANUS_ARGS="-jar $APP_ROOT/lib/spring-ai-alibaba-jmanus.jar"
    EXECUTION_MODE="jar"
else
    # Legacy native executable mode
    JMANUS_EXECUTABLE="$SCRIPT_DIR/bin/spring-ai-alibaba-jmanus"
    JMANUS_ARGS=""
    EXECUTION_MODE="native"
fi

# Check if the executable/app exists
if [ "$EXECUTION_MODE" = "exploded" ]; then
    if [ ! -d "$APP_DIR/BOOT-INF" ]; then
        echo "Error: Exploded jar directory not found at $APP_DIR"
        echo "Please make sure the application is properly installed."
        exit 1
    fi
    # Test java executable
    if ! "$JAVA_EXECUTABLE" -version >/dev/null 2>&1; then
        echo "Error: Java executable not working: $JAVA_EXECUTABLE"
        echo "Please check your Java installation."
        exit 1
    fi
elif [ "$EXECUTION_MODE" = "jar" ]; then
    if [ ! -f "$APP_ROOT/lib/spring-ai-alibaba-jmanus.jar" ]; then
        echo "Error: JManus jar file not found at $APP_ROOT/lib/spring-ai-alibaba-jmanus.jar"
        echo "Please make sure the application is properly installed."
        exit 1
    fi
    # Test java executable
    if ! "$JAVA_EXECUTABLE" -version >/dev/null 2>&1; then
        echo "Error: Java executable not working: $JAVA_EXECUTABLE"
        echo "Please check your Java installation."
        exit 1
    fi
else
    if [ ! -f "$JMANUS_EXECUTABLE" ]; then
        echo "Error: JManus executable not found at $JMANUS_EXECUTABLE"
        echo "Please make sure the application is properly installed."
        exit 1
    fi
    # Make sure the executable has proper permissions
    chmod +x "$JMANUS_EXECUTABLE"
fi

# Function to prompt for API key
prompt_for_api_key() {
    echo "==========================================="
    echo "       Welcome to JManus Application"
    echo "==========================================="
    echo ""
    echo "To use JManus, you need to provide your DashScope API Key."
    echo "You can get your API key from: https://dashscope.console.aliyun.com/"
    echo ""

    # Check if API key is already set in environment
    if [ -n "$DASHSCOPE_API_KEY" ]; then
        echo "Using existing DASHSCOPE_API_KEY from environment."
        return 0
    fi

    # Prompt for API key
    while [ -z "$DASHSCOPE_API_KEY" ]; do
        echo -n "Please enter your DashScope API Key: "
        read -s DASHSCOPE_API_KEY
        echo ""

        if [ -z "$DASHSCOPE_API_KEY" ]; then
            echo "API Key cannot be empty. Please try again."
            echo ""
        fi
    done

    export DASHSCOPE_API_KEY
}

# Function to start JManus
start_jmanus() {
    echo ""
    echo "Starting JManus application..."
    echo "Execution mode: $EXECUTION_MODE"
    if [ "$EXECUTION_MODE" = "exploded" ]; then
        echo "Using Java: $JAVA_EXECUTABLE"
        echo "App directory: $APP_DIR"
        echo "Main class: $MAIN_CLASS"
    elif [ "$EXECUTION_MODE" = "jar" ]; then
        echo "Using Java: $JAVA_EXECUTABLE"
        echo "Jar file: $APP_ROOT/lib/spring-ai-alibaba-jmanus.jar"
    else
        echo "Using native executable: $JMANUS_EXECUTABLE"
    fi
    echo "Application will be available at: http://localhost:18080"
    echo ""
    echo "Press Ctrl+C to stop the application."
    echo ""

    # Change to app directory for exploded jar mode
    if [ "$EXECUTION_MODE" = "exploded" ]; then
        cd "$APP_DIR"
    fi

    # Start the application
    if [ "$EXECUTION_MODE" = "exploded" ] || [ "$EXECUTION_MODE" = "jar" ]; then
        "$JAVA_EXECUTABLE" $JMANUS_ARGS
    else
        "$JMANUS_EXECUTABLE"
    fi
}

# Main execution
main() {
    # Detect if running from GUI (double-click) or terminal
    if [ -z "$TERM" ] || [ "$TERM" = "dumb" ]; then
        # Running from GUI (double-click), open a new Terminal window
        osascript -e "tell application \"Terminal\" to do script \"cd '$SCRIPT_DIR' && '$0' --terminal\""
        exit 0
    elif [ "$1" = "--terminal" ]; then
        # Running in Terminal window opened by osascript
        clear
        cd "$SCRIPT_DIR"
        prompt_for_api_key
        start_jmanus
    else
        # Running from existing terminal
        clear
        cd "$SCRIPT_DIR"
        prompt_for_api_key
        start_jmanus
    fi
}

# Handle script termination
trap 'echo ""; echo "JManus application stopped."; exit 0' INT TERM

# Run main function
main "$@"

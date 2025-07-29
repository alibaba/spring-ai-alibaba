@echo off
REM /*
REM  * Copyright 2025 the original author or authors.
REM  *
REM  * Licensed under the Apache License, Version 2.0 (the "License");
REM  * you may not use this file except in compliance with the License.
REM  * You may obtain a copy of the License at
REM  *
REM  *      https://www.apache.org/licenses/LICENSE-2.0
REM  *
REM  * Unless required by applicable law or agreed to in writing, software
REM  * distributed under the License is distributed on an "AS IS" BASIS,
REM  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  * See the License for the specific language governing permissions and
REM  * limitations under the License.
REM  */

REM Build frontend UI and deploy to Spring Boot static resources directory
REM For Windows

setlocal enabledelayedexpansion

REM Enable detailed logging
echo [DEBUG] Script started at %date% %time%
echo [DEBUG] Command line arguments: %*
echo [DEBUG] Current directory: %cd%

REM Get the absolute path of the script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"

echo [DEBUG] SCRIPT_DIR=%SCRIPT_DIR%
echo [DEBUG] PROJECT_ROOT=%PROJECT_ROOT%

REM Define color output (Windows doesn't support ANSI colors by default, but we'll keep the structure)
set "RED="
set "GREEN="
set "YELLOW="
set "BLUE="
set "NC="

REM Output colored logs (simplified for Windows)
:log_info
echo [INFO] %~1
echo [DEBUG] %~1
goto :eof

:log_success
echo [SUCCESS] %~1
echo [DEBUG] %~1
goto :eof

:log_warning
echo [WARNING] %~1
echo [DEBUG] %~1
goto :eof

:log_error
echo [ERROR] %~1
echo [DEBUG] %~1
goto :eof

:log_debug
echo [DEBUG] %~1
goto :eof

REM Check if necessary commands exist
:check_commands
call :log_info "Checking necessary commands..."
call :log_debug "Checking pnpm command..."

where pnpm >nul 2>&1
set "PNPM_CHECK_RESULT=%errorlevel%"
call :log_debug "pnpm check result: %PNPM_CHECK_RESULT%"

if %PNPM_CHECK_RESULT% neq 0 (
    call :log_error "pnpm is not installed, please install pnpm first"
    call :log_info "Installation command: npm install -g pnpm"
    call :log_debug "Command check failed - pnpm not found"
    exit /b 1
)

call :log_debug "pnpm found successfully"
call :log_success "Command check completed"
goto :eof

REM Check directory structure
:check_directories
call :log_info "Checking project directory structure..."
call :log_debug "Checking ui-vue3 directory..."

if not exist "%PROJECT_ROOT%ui-vue3" (
    call :log_error "ui-vue3 directory does not exist: %PROJECT_ROOT%ui-vue3"
    call :log_debug "Directory check failed - ui-vue3 not found"
    exit /b 1
)
call :log_debug "ui-vue3 directory exists"

call :log_debug "Checking ui-vue3\package.json file..."
if not exist "%PROJECT_ROOT%ui-vue3\package.json" (
    call :log_error "ui-vue3\package.json file does not exist"
    call :log_debug "File check failed - package.json not found"
    exit /b 1
)
call :log_debug "package.json file exists"

call :log_debug "Checking Spring Boot resources directory..."
if not exist "%PROJECT_ROOT%src\main\resources" (
    call :log_error "Spring Boot resources directory does not exist: %PROJECT_ROOT%src\main\resources"
    call :log_debug "Directory check failed - resources directory not found"
    exit /b 1
)
call :log_debug "Spring Boot resources directory exists"

call :log_success "Directory structure check completed"
goto :eof

REM Build frontend project
:build_frontend
call :log_info "Starting frontend project build..."
call :log_debug "Changing to ui-vue3 directory..."

cd /d "%PROJECT_ROOT%ui-vue3"
set "CURRENT_DIR=%cd%"
call :log_debug "Current directory after change: %CURRENT_DIR%"

REM Check if node_modules exists, install dependencies if not
call :log_debug "Checking for node_modules directory..."
if not exist "node_modules" (
    call :log_warning "node_modules does not exist, installing dependencies..."
    call :log_debug "Running pnpm install..."
    pnpm install
    set "INSTALL_RESULT=%errorlevel%"
    call :log_debug "pnpm install result: %INSTALL_RESULT%"
    
    if %INSTALL_RESULT% neq 0 (
        call :log_error "Failed to install dependencies"
        call :log_debug "Dependency installation failed"
        exit /b 1
    )
    call :log_debug "Dependencies installed successfully"
) else (
    call :log_debug "node_modules directory exists, skipping installation"
)

REM Run build command
call :log_info "Running pnpm run build..."
call :log_debug "Executing pnpm run build command..."
pnpm run build
set "BUILD_RESULT=%errorlevel%"
call :log_debug "pnpm run build result: %BUILD_RESULT%"

if %BUILD_RESULT% neq 0 (
    call :log_error "Build failed"
    call :log_debug "Build process failed with error code: %BUILD_RESULT%"
    exit /b 1
)

REM Check build result
call :log_debug "Checking for ui directory after build..."
if not exist "ui" (
    call :log_error "Build failed, ui directory does not exist"
    call :log_debug "Build verification failed - ui directory not found"
    call :log_debug "Current directory contents:"
    dir
    exit /b 1
)
call :log_debug "ui directory found after build"

call :log_success "Frontend project build completed"
goto :eof

REM Clear static resources directory
:clean_static_directory
call :log_info "Clearing static resources directory..."

set "STATIC_DIR=%PROJECT_ROOT%src\main\resources\static"
call :log_debug "Static directory path: %STATIC_DIR%"

REM Create directory (if it doesn't exist)
call :log_debug "Checking if static directory exists..."
if not exist "%STATIC_DIR%" (
    call :log_debug "Creating static directory..."
    mkdir "%STATIC_DIR%"
    set "MKDIR_RESULT=%errorlevel%"
    call :log_debug "mkdir result: %MKDIR_RESULT%"
    if %MKDIR_RESULT% neq 0 (
        call :log_error "Failed to create static directory"
        exit /b 1
    )
    call :log_debug "Static directory created successfully"
) else (
    call :log_debug "Static directory already exists"
)

REM Clear directory contents
call :log_debug "Checking if static directory has contents..."
dir /b "%STATIC_DIR%" >nul 2>&1
set "DIR_CHECK_RESULT=%errorlevel%"
call :log_debug "Directory content check result: %DIR_CHECK_RESULT%"

if %DIR_CHECK_RESULT% equ 0 (
    call :log_warning "Deleting existing files in %STATIC_DIR%..."
    call :log_debug "Deleting files..."
    del /q "%STATIC_DIR%\*" >nul 2>&1
    set "DEL_RESULT=%errorlevel%"
    call :log_debug "del result: %DEL_RESULT%"
    
    call :log_debug "Deleting subdirectories..."
    for /d %%i in ("%STATIC_DIR%\*") do (
        call :log_debug "Removing directory: %%i"
        rmdir /s /q "%%i" >nul 2>&1
    )
    call :log_success "Static resources directory cleared"
) else (
    call :log_info "Static resources directory is already empty"
)
goto :eof

REM Copy build files
:copy_build_files
call :log_info "Copying build files to static resources directory..."

set "SOURCE_DIR=%PROJECT_ROOT%ui-vue3\ui"
set "TARGET_DIR=%PROJECT_ROOT%src\main\resources\static\"

call :log_debug "Source directory: %SOURCE_DIR%"
call :log_debug "Target directory: %TARGET_DIR%"

if not exist "%SOURCE_DIR%" (
    call :log_error "Source directory does not exist: %SOURCE_DIR%"
    call :log_debug "Source directory check failed"
    exit /b 1
)
call :log_debug "Source directory exists"

REM Copy files
call :log_debug "Starting file copy with xcopy..."
xcopy "%SOURCE_DIR%" "%TARGET_DIR%" /e /i /y >nul
set "XCOPY_RESULT=%errorlevel%"
call :log_debug "xcopy result: %XCOPY_RESULT%"

if %XCOPY_RESULT% neq 0 (
    call :log_error "File copy failed"
    call :log_debug "File copy failed with error code: %XCOPY_RESULT%"
    exit /b 1
)

REM Verify copy result
call :log_debug "Verifying copy result..."
dir /b "%TARGET_DIR%" >nul 2>&1
set "VERIFY_RESULT=%errorlevel%"
call :log_debug "Verification result: %VERIFY_RESULT%"

if %VERIFY_RESULT% equ 0 (
    call :log_success "File copy completed"
    call :log_info "Copied files:"
    call :log_debug "Listing copied files:"
    dir "%TARGET_DIR%"
) else (
    call :log_error "File copy failed, target directory is empty"
    call :log_debug "Copy verification failed"
    exit /b 1
)
goto :eof

REM Show build summary
:show_summary
call :log_success "=== Build Completed ==="
call :log_info "Frontend files successfully deployed to: %PROJECT_ROOT%src\main\resources\static\"
call :log_info "You can now run the Spring Boot application"
call :log_info ""
call :log_info "Example startup commands:"
call :log_info "  mvn spring-boot:run"
call :log_info "  or"
call :log_info "  java -jar target\spring-ai-alibaba-jmanus-*.jar"
call :log_debug "Build summary displayed"
goto :eof

REM Main function
:main
call :log_info "Starting frontend build and deployment process..."
call :log_info "Project root directory: %PROJECT_ROOT%"
call :log_debug "Main function started"
echo.

call :check_commands
if %errorlevel% neq 0 (
    call :log_debug "check_commands failed, exiting"
    exit /b 1
)

call :check_directories
if %errorlevel% neq 0 (
    call :log_debug "check_directories failed, exiting"
    exit /b 1
)

call :build_frontend
if %errorlevel% neq 0 (
    call :log_debug "build_frontend failed, exiting"
    exit /b 1
)

call :clean_static_directory
if %errorlevel% neq 0 (
    call :log_debug "clean_static_directory failed, exiting"
    exit /b 1
)

call :copy_build_files
if %errorlevel% neq 0 (
    call :log_debug "copy_build_files failed, exiting"
    exit /b 1
)

call :show_summary

call :log_success "All steps completed!"
call :log_debug "Main function completed successfully"
goto :eof

REM Run main function
call :log_debug "Starting main execution..."
call :main
set "MAIN_RESULT=%errorlevel%"
call :log_debug "Main function result: %MAIN_RESULT%"

if %MAIN_RESULT% neq 0 (
    call :log_error "Error occurred during script execution, exit code: %MAIN_RESULT%"
    call :log_debug "Script execution failed with error code: %MAIN_RESULT%"
    exit /b 1
)

call :log_debug "Script completed successfully at %date% %time%" 
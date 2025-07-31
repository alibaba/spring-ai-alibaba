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
setlocal enabledelayedexpansion
REM 设置颜色变量（需要支持ANSI的终端）
for /f %%a in ('echo prompt $E^| cmd') do set "ESC=%%a"
set "RED=%ESC%[0;31m"
set "GREEN=%ESC%[0;32m"
set "YELLOW=%ESC%[1;33m"
set "BLUE=%ESC%[0;34m"
set "NC=%ESC%[0m"
REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
REM 主执行流程
call:main
exit /b 0
:main
    call:log_info "Starting frontend build and deployment process..."
    call:log_info "Project root directory: %PROJECT_ROOT%"
    echo.
    
    call:check_commands
    call:check_directories
    call:build_frontend
    call:clean_static_directory
    call:copy_build_files
    call:show_summary
    
    call:log_success "All steps completed!"
goto:eof
:log_info
    echo %BLUE%[INFO]%NC% %*
goto:eof
:log_success
    echo %GREEN%[SUCCESS]%NC% %*
goto:eof
:log_warning
    echo %YELLOW%[WARNING]%NC% %*
goto:eof
:log_error
    echo %RED%[ERROR]%NC% %*
goto:eof
:check_commands
    call:log_info "Checking necessary commands..."
    
    where pnpm >nul 2>nul
    if %errorlevel% neq 0 (
        call:log_error "pnpm is not installed, please install pnpm first"
        call:log_info "Installation command: npm install -g pnpm"
        exit /b 1
    )
    call:log_success "Command check completed"
goto:eof
:check_directories
    call:log_info "Checking project directory structure..."
    
    if not exist "%PROJECT_ROOT%\ui-vue3\" (
        call:log_error "ui-vue3 directory does not exist: %PROJECT_ROOT%\ui-vue3"
        exit /b 1
    )
    
    if not exist "%PROJECT_ROOT%\ui-vue3\package.json" (
        call:log_error "ui-vue3\package.json file does not exist"
        exit /b 1
    )
    
    if not exist "%PROJECT_ROOT%\src\main\resources\" (
        call:log_error "Spring Boot resources directory does not exist: %PROJECT_ROOT%\src\main\resources"
        exit /b 1
    )
    
    call:log_success "Directory structure check completed"
goto:eof
:build_frontend
    call:log_info "Starting frontend project build..."
    
    cd /d "%PROJECT_ROOT%\ui-vue3"
    
    if not exist "node_modules\" (
        call:log_warning "node_modules does not exist, installing dependencies..."
        pnpm install || (
            call:log_error "Failed to install dependencies"
            exit /b 1
        )
    )
    
    call:log_info "Running pnpm run build..."
    pnpm run build || (
        call:log_error "Build failed"
        exit /b 1
    )
    
    if not exist "ui\" (
        call:log_error "Build failed, ui directory does not exist"
        exit /b 1
    )
    
    call:log_success "Frontend project build completed"
goto:eof
:clean_static_directory
    call:log_info "Clearing static resources directory..."
    
    set "STATIC_DIR=%PROJECT_ROOT%\src\main\resources\static"
    
    if not exist "%STATIC_DIR%\" (
        mkdir "%STATIC_DIR%" || (
            call:log_error "Failed to create static directory"
            exit /b 1
        )
    )
    
    dir "%STATIC_DIR%\*" >nul 2>nul
    if %errorlevel% == 0 (
        call:log_warning "Deleting existing files in %STATIC_DIR%..."
        del /q "%STATIC_DIR%\*" >nul 2>nul
        for /d %%d in ("%STATIC_DIR%\*") do rd /s /q "%%d" >nul 2>nul
        call:log_success "Static resources directory cleared"
    ) else (
        call:log_info "Static resources directory is already empty"
    )
goto:eof
:copy_build_files
    call:log_info "Copying build files to static resources directory..."
    
    set "SOURCE_DIR=%PROJECT_ROOT%\ui-vue3\ui"
    set "TARGET_DIR=%PROJECT_ROOT%\src\main\resources\static\ui\"
    
    if not exist "%SOURCE_DIR%\" (
        call:log_error "Source directory does not exist: %SOURCE_DIR%"
        exit /b 1
    )
    
    xcopy /e /i /y "%SOURCE_DIR%\*" "%TARGET_DIR%" >nul || (
        call:log_error "File copy failed"
        exit /b 1
    )
    
    dir "%TARGET_DIR%\*" >nul 2>nul
    if %errorlevel% == 0 (
        call:log_success "File copy completed"
        call:log_info "Copied files:"
        dir /b "%TARGET_DIR%"
    ) else (
        call:log_error "File copy failed, target directory is empty"
        exit /b 1
    )
goto:eof
:show_summary
    call:log_success "=== Build Completed ==="
    call:log_info "Frontend files successfully deployed to: %PROJECT_ROOT%\src\main\resources\static\"
    call:log_info "You can now run the Spring Boot application"
    echo.
    call:log_info "Example startup commands:"
    call:log_info "  mvn spring-boot:run"
    call:log_info "  or"
    call:log_info "  java -jar target\spring-ai-alibaba-jmanus-*.jar"
goto:eof

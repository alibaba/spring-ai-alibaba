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

@echo off
setlocal enabledelayedexpansion

set JAR_NAME=spring-ai-alibaba-jmanus-0.0.9.jar
set LOG_DIR=logs
set JAVA_OPTS=-Xms256m -Xmx512m

:: 检查是否已运行
for /f "delims=" %%p in ('powershell -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'javaw.exe' -and $_.CommandLine -like '*%JAR_NAME%*' } | Select-Object -ExpandProperty ProcessId"') do (
    echo Application %JAR_NAME% is already running with PID %%p. Startup aborted.
    goto :eof
)

:: 时间格式处理
for /f "tokens=1-3 delims=/ " %%a in ("%date%") do (
    set yyyy=%%c
    set mm=%%a
    set dd=%%b
)
for /f "tokens=1-2 delims=:." %%a in ("%time%") do (
    set hh=%%a
    set min=%%b
)
set LOG_DATE=%yyyy%-%mm%-%dd%_%hh%%min%

:: 日志目录和备份处理
if not exist %LOG_DIR% (
    mkdir %LOG_DIR%
)

if exist %LOG_DIR%\info.log (
    ren %LOG_DIR%\info.log info_!LOG_DATE!.log
)
if exist %LOG_DIR%\error.log (
    ren %LOG_DIR%\error.log error_!LOG_DATE!.log
)

:: 启动后台程序（用 javaw，不做外部日志重定向）
start "" javaw %JAVA_OPTS% -jar %JAR_NAME%

echo Application started with javaw.
echo Logs should be handled inside your application.
endlocal

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
setlocal

set JAR_NAME=spring-ai-alibaba-jmanus-0.0.9.jar

:: 用 PowerShell 查找包含 JAR 名称的 javaw.exe 进程
for /f "delims=" %%p in ('powershell -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'javaw.exe' -and $_.CommandLine -like '*%JAR_NAME%*' } | Select-Object -ExpandProperty ProcessId"') do (
    echo Found javaw.exe running with PID %%p that matches %JAR_NAME%
    taskkill /PID %%p /F
    echo Process %%p terminated.
    goto :eof
)

echo No matching javaw.exe process found for %JAR_NAME%
endlocal

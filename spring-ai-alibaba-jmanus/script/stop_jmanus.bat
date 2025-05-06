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

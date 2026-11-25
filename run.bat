@echo off
set "JAVA_HOME=C:\Users\Yash Varmora\tools\jdk"
set "PATH=%JAVA_HOME%\bin;C:\Users\Yash Varmora\tools\maven\bin;%PATH%"

echo ========================================================
echo   StyleHub Spring Boot E-Commerce Application
echo   Storefront:  http://localhost:8085/
echo   Network IP:  http://192.168.1.14:8085/
echo   Admin Panel: http://localhost:8085/admin
echo   Admin User:  admin@stylehub.local / Admin@12345
echo   Cust User:   customer@stylehub.local / Customer@12345
echo ========================================================

REM Free port 8085 if already in use by a previous instance
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8085 ^| findstr LISTENING') do (
    echo Stopping previous process on port 8085 (PID %%a)...
    taskkill /F /PID %%a >nul 2>&1
)

if exist target\stylehub-1.0.0.jar (
    java -jar target\stylehub-1.0.0.jar
) else (
    call mvn spring-boot:run
)
pause

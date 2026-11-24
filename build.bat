@echo off
set "JAVA_HOME=C:\Users\Yash Varmora\tools\jdk"
set "PATH=%JAVA_HOME%\bin;C:\Users\Yash Varmora\tools\maven\bin;%PATH%"
echo ========================================================
echo  Packaging StyleHub Runnable Production JAR
echo ========================================================
mvn clean package -DskipTests
pause

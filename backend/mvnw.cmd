@echo off
setlocal
set "BASE_DIR=%~dp0"
if "%BASE_DIR:~-1%"=="\" set "BASE_DIR=%BASE_DIR:~0,-1%"
set "JAR=%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%JAR%" (
  echo Downloading maven-wrapper.jar ...
  curl -sSL -o "%JAR%" https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
)
java "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" -cp "%JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal

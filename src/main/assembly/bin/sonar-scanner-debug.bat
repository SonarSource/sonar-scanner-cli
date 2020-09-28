@REM SonarScanner Startup Script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - Location of Java's installation, optional if use_embedded_jre is set
@REM
@REM Optional ENV vars:
@REM   SONAR_SCANNER_OPTS - parameters passed to the Java VM when running the SonarScanner

@setlocal
@set SONAR_SCANNER_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
echo "Executing SonarScanner in Debug Mode"
echo "SONAR_SCANNER_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
@call "%~dp0"sonar-scanner.bat %*

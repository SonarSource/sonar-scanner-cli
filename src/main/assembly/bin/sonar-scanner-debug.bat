@REM SonarScanner CLI Startup Script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - Location of Java's installation, optional if use_embedded_jre is set
@REM
@REM Optional ENV vars:
@REM   SONAR_SCANNER_OPTS - parameters passed to the Java VM when running the SonarScanner

@setlocal
@set SONAR_SCANNER_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@set SONAR_SCANNER_JAVA_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8001

@set SONAR_SCANNER_JAVA_OPTS=$SONAR_SCANNER_JAVA_OPTS $SONAR_SCANNER_JAVA_DEBUG_OPTS

echo "Executing SonarScanner CLI in Debug Mode"
echo "SONAR_SCANNER_DEBUG_OPTS=%SONAR_SCANNER_DEBUG_OPTS%"
echo "SONAR_SCANNER_JAVA_OPTS=%SONAR_SCANNER_JAVA_OPTS%"
@call "%~dp0"sonar-scanner.bat %*

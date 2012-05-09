@REM Sonar Runner Startup Script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars:
@REM   SONAR_RUNNER_HOME - location of runner's installed home dir
@REM   SONAR_RUNNER_OPTS - parameters passed to the Java VM when running Sonar

@echo off


@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJavaHome

echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto end

:OkJavaHome
if NOT "%SONAR_RUNNER_HOME%"=="" goto cleanSonarRunnerHome
set SONAR_RUNNER_HOME=%~dp0..
goto run

:cleanSonarRunnerHome
@REM If the property has a trailing backslash, remove it
if %SONAR_RUNNER_HOME:~-1%==\ set SONAR_RUNNER_HOME=%SONAR_RUNNER_HOME:~0,-1%


@REM ==== START RUN ====
:run
echo %SONAR_RUNNER_HOME%

set PROJECT_HOME=%CD%

"%JAVA_HOME%\bin\java.exe" %SONAR_RUNNER_OPTS% -classpath "%SONAR_RUNNER_HOME%\lib\sonar-runner.jar";"%SONAR_RUNNER_HOME%\lib\sonar-batch-bootstrapper.jar" "-Drunner.home=%SONAR_RUNNER_HOME%" "-Dproject.home=%PROJECT_HOME%" org.sonar.runner.Main %*


@REM ==== END EXECUTION ====
:end

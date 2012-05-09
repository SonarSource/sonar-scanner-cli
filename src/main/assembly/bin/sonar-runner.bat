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
@REM => JAVA EXEC
if not "%JAVA_HOME%" == "" goto foundJavaHome

for %%i in (java.exe) do set JAVA_EXEC=%%~$PATH:i

if not "%JAVA_EXEC%" == "" goto OkJava

echo.
echo ERROR: JAVA_HOME not found in your environment, and no Java 
echo        executable present in the PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation, or add "java.exe" to the PATH
echo.
goto end

:foundJavaHome
if EXIST "%JAVA_HOME%\bin\java.exe" goto foundJavaExeFromJavaHome

echo.
echo ERROR: JAVA_HOME exists but does not point to a valid Java home
echo        folder. No "\bin\java.exe" file can be found there.
echo.
goto end

:foundJavaExeFromJavaHome
set JAVA_EXEC="%JAVA_HOME%\bin\java.exe"

:OkJava
if NOT "%SONAR_RUNNER_HOME%"=="" goto cleanSonarRunnerHome
set SONAR_RUNNER_HOME=%~dp0..
goto run

@REM => SONAR_RUNNER_HOME
:cleanSonarRunnerHome
@REM If the property has a trailing backslash, remove it
if %SONAR_RUNNER_HOME:~-1%==\ set SONAR_RUNNER_HOME=%SONAR_RUNNER_HOME:~0,-1%



@REM ==== START RUN ====
:run
echo %SONAR_RUNNER_HOME%

set PROJECT_HOME=%CD%

%JAVA_EXEC% %SONAR_RUNNER_OPTS% -classpath "%SONAR_RUNNER_HOME%\lib\sonar-runner.jar";"%SONAR_RUNNER_HOME%\lib\sonar-batch-bootstrapper.jar" "-Drunner.home=%SONAR_RUNNER_HOME%" "-Dproject.home=%PROJECT_HOME%" org.sonar.runner.Main %*



@REM ==== END EXECUTION ====
:end

@REM SonarQube Runner Startup Script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars:
@REM   SONAR_RUNNER_HOME - location of runner's installed home dir
@REM   SONAR_RUNNER_OPTS - parameters passed to the Java VM when running Sonar

@echo off

set ERROR_CODE=0

@REM set local scope for the variables with windows NT shell
@setlocal

@REM ==== START VALIDATION ====
@REM *** JAVA EXEC VALIDATION ***
if not "%JAVA_HOME%" == "" goto foundJavaHome

for %%i in (java.exe) do set JAVA_EXEC=%%~$PATH:i

if not "%JAVA_EXEC%" == "" (
  set JAVA_EXEC="%JAVA_EXEC%"
  goto OkJava
)

if not "%JAVA_EXEC%" == "" goto OkJava

echo.
echo ERROR: JAVA_HOME not found in your environment, and no Java
echo        executable present in the PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation, or add "java.exe" to the PATH
echo.
goto error

:foundJavaHome
if EXIST "%JAVA_HOME%\bin\java.exe" goto foundJavaExeFromJavaHome

echo.
echo ERROR: JAVA_HOME exists but does not point to a valid Java home
echo        folder. No "\bin\java.exe" file can be found there.
echo.
goto error

:foundJavaExeFromJavaHome
set JAVA_EXEC="%JAVA_HOME%\bin\java.exe"

@REM *** SONAR RUNNER HOME VALIDATION ***
:OkJava
if NOT "%SONAR_RUNNER_HOME%"=="" goto cleanSonarRunnerHome
set SONAR_RUNNER_HOME=%~dp0..
goto run

:cleanSonarRunnerHome
@REM If the property has a trailing backslash, remove it
if "%SONAR_RUNNER_HOME:~-1%"=="\" set SONAR_RUNNER_HOME=%SONAR_RUNNER_HOME:~0,-1%

@REM Check if the provided SONAR_RUNNER_HOME is a valid install dir
IF EXIST "%SONAR_RUNNER_HOME%\lib\sonar-runner-dist-${project.version}.jar" goto run

echo.
echo ERROR: SONAR_RUNNER_HOME exists but does not point to a valid install
echo        directory: %SONAR_RUNNER_HOME%
echo.
goto error



@REM ==== START RUN ====
:run
echo %SONAR_RUNNER_HOME%

set PROJECT_HOME=%CD%

%JAVA_EXEC% -Djava.awt.headless=true %SONAR_RUNNER_OPTS% -cp "%SONAR_RUNNER_HOME%\lib\sonar-runner-dist-${project.version}.jar" "-Drunner.home=%SONAR_RUNNER_HOME%" "-Dproject.home=%PROJECT_HOME%" org.sonar.runner.cli.Main %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

@REM ==== END EXECUTION ====

:end
@REM set local scope for the variables with windows NT shell
@endlocal & set ERROR_CODE=%ERROR_CODE%

@REM see http://code-bear.com/bearlog/2007/06/01/getting-the-exit-code-from-a-batch-file-that-is-run-from-a-python-program/
goto exit

:returncode
exit /B %1

:exit
call :returncode %ERROR_CODE%

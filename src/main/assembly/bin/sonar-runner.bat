@REM Sonar Runner Startup Script for Windows
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM   SONAR_RUNNER_HOME - location of runner's installed home dir
@REM
@REM Optional ENV vars:
@REM   SONAR_RUNNER_OPTS - parameters passed to the Java VM when running Sonar

@echo off

"%JAVA_HOME%\bin\java.exe" %SONAR_RUNNER_OPTS% -classpath "%SONAR_RUNNER_HOME%\lib\sonar-runner.jar";"%SONAR_RUNNER_HOME%\lib\sonar-batch-bootstrapper.jar" "-Drunner.home=%SONAR_RUNNER_HOME%" org.sonar.runner.Main %*

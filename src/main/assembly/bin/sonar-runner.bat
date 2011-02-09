@REM Sonar Runner Startup Script for Windows

@echo off

"%JAVA_HOME%\bin\java.exe" -classpath "%SONAR_RUNNER_HOME%\lib\sonar-runner.jar";"%SONAR_RUNNER_HOME%\lib\sonar-batch-bootstrapper.jar" "-Drunner.home=%SONAR_RUNNER_HOME%" org.sonar.runner.Main %*

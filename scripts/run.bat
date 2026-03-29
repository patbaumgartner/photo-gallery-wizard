@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

for %%f in ("%SCRIPT_DIR%photo-gallery-wizard-*.jar") do (
    java -jar "%%f" %*
    exit /b %ERRORLEVEL%
)

echo ERROR: No photo-gallery-wizard JAR found in %SCRIPT_DIR% >&2
exit /b 1

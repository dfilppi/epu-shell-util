@echo off
rem This script provides the command and control utility for the
rem GigaSpaces Technologies Inc. Service Grid

call "%~dp0\..\tools\groovy\bin\groovy.bat" "%~dp0\gs.groovy" %*

if %errorlevel% EQU 99 exit 0

if %errorlevel% NEQ 0 exit %errorlevel%

rem got this far, just call regular
call "%~dp0\gs.bat" %*

exit %errorlevel%

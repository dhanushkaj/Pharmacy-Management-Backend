@echo off
REM =============================================================================
REM Create Desktop Shortcut for Pharmacy Management System
REM Run this script ONCE as Administrator to create the desktop icon
REM =============================================================================

echo.
echo Creating Desktop Shortcut for Pharmacy Management System...
echo.

set SCRIPT_DIR=%~dp0
set SHORTCUT_NAME=Pharmacy System
set TARGET_BAT=%SCRIPT_DIR%start-pharmacy-app.bat
set DESKTOP=%USERPROFILE%\Desktop

REM Create VBScript to make the shortcut (Windows native method)
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
echo sLinkFile = "%DESKTOP%\%SHORTCUT_NAME%.lnk" >> "%TEMP%\CreateShortcut.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
echo oLink.TargetPath = "cmd.exe" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Arguments = "/c ""%TARGET_BAT%""" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WorkingDirectory = "%SCRIPT_DIR%" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Description = "Start Pharmacy Management System" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.IconLocation = "shell32.dll,21" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"

cscript //nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs"

REM Also create Stop shortcut
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut2.vbs"
echo sLinkFile = "%DESKTOP%\Stop Pharmacy System.lnk" >> "%TEMP%\CreateShortcut2.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.TargetPath = "cmd.exe" >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.Arguments = "/c ""%SCRIPT_DIR%stop-pharmacy-app.bat""" >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.WorkingDirectory = "%SCRIPT_DIR%" >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.Description = "Stop Pharmacy Management System" >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.IconLocation = "shell32.dll,27" >> "%TEMP%\CreateShortcut2.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut2.vbs"

cscript //nologo "%TEMP%\CreateShortcut2.vbs"
del "%TEMP%\CreateShortcut2.vbs"

echo.
echo  ============================================
echo     DESKTOP SHORTCUTS CREATED!
echo  ============================================
echo.
echo  Two shortcuts added to your Desktop:
echo    - "Pharmacy System" (green icon) - Start the app
echo    - "Stop Pharmacy System" (red icon) - Stop all servers
echo.
echo  You can now double-click "Pharmacy System" on your
echo  desktop to launch both frontend and backend!
echo.

pause

@ECHO OFF

rem Initializing the common environment for GigaSpaces(relevant for this example are %JSHOMEDIR%,%GS_JARS%,%LOOKUPGROUPS%)
@call "%~dp0\..\..\..\..\..\bin\setenv.bat"

rem Setting JARS (%JSHOMEDIR% is GigaSpaces root dir)
set JARS=%GS_JARS%

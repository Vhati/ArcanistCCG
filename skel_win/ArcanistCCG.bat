@ECHO OFF
SETLOCAL

CD /D "%~dp0"
java -cp "ArcanistCCG.jar" org.arcanist.client.ArcanistCCG

ENDLOCAL
EXIT /B

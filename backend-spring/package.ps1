# Build and package backend-spring into a zip (Windows PowerShell)
Set-StrictMode -Version Latest
Push-Location -Path (Split-Path -Parent $MyInvocation.MyCommand.Definition)
mvn -q clean package
$out = "../mcp-gateway-backend-spring.zip"
if (Test-Path $out) { Remove-Item $out }
Compress-Archive -Path .\target\*.jar, ..\src\main\resources\application.properties -DestinationPath $out
Write-Host "Packaged to $out"
Pop-Location

# Check if JAVA_HOME is set and print java version
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME is not set. Use setx JAVA_HOME 'C:\\Program Files\\Java\\jdk-17.0.x' and restart your shell." -ForegroundColor Yellow
} else {
    Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green
}

try {
    & $env:JAVA_HOME\bin\java -version
} catch {
    Write-Host "Could not execute java from JAVA_HOME. Ensure the path is correct." -ForegroundColor Red
}
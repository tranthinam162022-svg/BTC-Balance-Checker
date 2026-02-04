# Run tests using maven wrapper on Windows
try {
    & .\mvnw.cmd test
} catch {
    Write-Host "Failed to run mvnw.cmd. Ensure JAVA_HOME is set and mvnw.cmd is executable." -ForegroundColor Red
}
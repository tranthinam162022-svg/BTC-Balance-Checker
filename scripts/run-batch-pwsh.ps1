# Example PowerShell script to upload a CSV and poll status
$host = $env:HOST -or 'http://localhost:8080'
$file = 'addresses.txt'

# Upload file using Invoke-WebRequest to start job
$response = Invoke-RestMethod -Uri "$host/batch/upload" -Method Post -InFile $file -ContentType 'multipart/form-data'
Write-Host "Upload response:" $response

# Note: the controller returns a view with job id in the rendered HTML; simpler approach is to call the BatchService endpoint directly using REST API /api/balance/batch
# For direct API usage, use POST /api/balance/batch with JSON body as in run-batch-curl.sh

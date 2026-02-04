#!/usr/bin/env bash
# Start a job then cancel it (example)
HOST=${HOST:-http://localhost:8080}

# start job via REST batch
RESP=$(curl -s -X POST "$HOST/api/balance/batch" -H "Content-Type: application/json" -d '{"currency":"","offset":0,"addresses":["1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa","1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp","1BoatSLRHtKNngkdXEeobR76b53LETtpyT"]}')
# parse job id from response if present
echo "Response: $RESP"
# For cancel via controller: curl -X POST $HOST/batch/cancel/{jobId}
echo "Use the web UI / SSE or call /batch/cancel/{jobId} to cancel a job started by file upload."
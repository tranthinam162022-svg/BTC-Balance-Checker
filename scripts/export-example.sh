#!/usr/bin/env bash
# Export results example
HOST=${HOST:-http://localhost:8080}
JOBID="$1"
if [ -z "$JOBID" ]; then
  echo "Usage: $0 <jobId>"; exit 1
fi

# CSV
curl -s -o batch.csv "$HOST/batch/export/$JOBID?format=csv"
ls -l batch.csv && echo "Saved batch.csv"

# JSON
curl -s -o batch.json "$HOST/batch/export/$JOBID?format=json"
ls -l batch.json && echo "Saved batch.json"

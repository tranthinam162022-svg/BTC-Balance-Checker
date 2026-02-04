#!/usr/bin/env bash
# Example: send batch via REST API
HOST=${HOST:-http://localhost:8080}

curl -X POST "$HOST/api/balance/batch" \
  -H "Content-Type: application/json" \
  -d '{"currency":"","offset":0,"addresses":["1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa","1dice8EMZmqKvrGE4Qc9bUFf9PX3xaYDp"]}' | jq

# Example: upload CSV/TXT file via web controller
# curl -F "file=@addresses.txt" -F "currency=" -F "offset=0" "$HOST/batch/upload" -v

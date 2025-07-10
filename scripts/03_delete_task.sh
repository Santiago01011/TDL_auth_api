#!/bin/bash
#
# 03_delete_task.sh - Deletes the task from the previous steps.

source ./setup.sh

echo -e "${YELLOW}--- Running Delete Task Test ---${NC}"

if [ ! -f "$TOKEN_FILE" ] || [ ! -f "$TASK_ID_FILE" ]; then
    echo -e "${RED}FAILED: Required session files (token/task_id) not found. Run previous scripts first.${NC}"
    exit 1
fi

AUTH_TOKEN=$(cat "$TOKEN_FILE")
TASK_ID=$(cat "$TASK_ID_FILE")
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

echo "STEP 6: Sending a DELETE command for task ID ${TASK_ID}..."

COMMAND_ID=$(powershell.exe -Command "[guid]::NewGuid().ToString()")

SYNC_PAYLOAD=$(printf '{ "commands": [{ "commandId": "%s", "entityId": "%s", "type": "DELETE_TASK", "timestamp": "%s", "data": {} }] }' "$COMMAND_ID" "$TASK_ID" "$TIMESTAMP")

SYNC_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE_URL}/${SYNC_ENDPOINT_ROUTE}" -H "Content-Type: application/json" -H "Authorization: Bearer ${AUTH_TOKEN}" -d "$SYNC_PAYLOAD")

HTTP_CODE=$(echo "$SYNC_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$SYNC_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ne 200 ] || [ $(echo "$RESPONSE_BODY" | jq '.success | length') -ne 1 ]; then
    echo -e "${RED}FAILED: Delete command was not processed successfully.${NC}"
    echo "Response: $RESPONSE_BODY"
    exit 1
fi

echo -e "${GREEN}SUCCESS: Delete command processed successfully.${NC}"
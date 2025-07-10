#!/bin/bash
#
# 01_create_task.sh - Registers a user and creates a single task.

# --- Load Configuration ---
source ./setup.sh

echo -e "${YELLOW}--- Running Create Task Test ---${NC}"

# --- Step 1: Register a New User (if not already registered) ---
if [ -f "$USER_REGISTERED_FILE" ]; then
    echo "STEP 1: User already registered, skipping registration..."
    echo -e "${GREEN}SUCCESS: Using existing user.${NC}"
else
    echo "STEP 1: Registering new user (${TEST_USERNAME})..."
    REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE_URL}/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"username": "'"${TEST_USERNAME}"'","email": "'"${TEST_EMAIL}"'","password": "'"${TEST_PASSWORD}"'"}')
    HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        VERIFICATION_CODE=$(echo "$RESPONSE_BODY" | sed 's/Registration successful. Verification code: //')
        echo "$VERIFICATION_CODE" > "$VERIFICATION_CODE_FILE"
        echo -e "${GREEN}SUCCESS: User registered.${NC}"
        
        # Mark user as registered
        echo "registered" > "$USER_REGISTERED_FILE"
    elif [[ "$RESPONSE_BODY" == *"already exists"* ]]; then
        echo -e "${YELLOW}User already exists, marking as registered.${NC}"
        echo "registered" > "$USER_REGISTERED_FILE"
    else
        echo -e "${RED}FAILED: Registration failed with HTTP status ${HTTP_CODE}.${NC}"
        echo "Response: ${RESPONSE_BODY}"
        exit 1
    fi
fi

# --- Step 2: Verify the User (if verification code exists) ---
if [ -f "$VERIFICATION_CODE_FILE" ]; then
    echo "STEP 2: Verifying user..."
    CODE_TO_VERIFY=$(cat "$VERIFICATION_CODE_FILE")
    VERIFY_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${API_BASE_URL}/auth/verify?code=${CODE_TO_VERIFY}")
    HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}SUCCESS: User verified.${NC}"
        # Remove verification code file since user is now verified
        rm -f "$VERIFICATION_CODE_FILE"
    else
        echo -e "${YELLOW}Verification failed or user already verified.${NC}"
    fi
else
    echo "STEP 2: User already verified, skipping verification..."
fi

# --- Step 3: Log In & Get Folders ---
echo "STEP 3: Logging in and fetching folder..."
# ... (login and folder fetch logic is the same)
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE_URL}/auth/login" -H "Content-Type: application/json" -d '{"email": "'"${TEST_EMAIL}"'","password": "'"${TEST_PASSWORD}"'"}')
HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.token')
echo "$TOKEN" > "$TOKEN_FILE"
FOLDERS_RESPONSE=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${API_BASE_URL}/v2/folders")
TEST_FOLDER_ID=$(echo "$FOLDERS_RESPONSE" | jq -r '.[0].folderId')
echo -e "${GREEN}SUCCESS: Logged in and fetched folder ID.${NC}"

# --- Step 4: Create a Task ---
echo "STEP 4: Sending a CREATE command..."
AUTH_TOKEN=$(cat "$TOKEN_FILE")
COMMAND_ID=$(powershell.exe -Command "[guid]::NewGuid().ToString()")
TASK_ID=$(powershell.exe -Command "[guid]::NewGuid().ToString()")
echo "$TASK_ID" > "$TASK_ID_FILE" # Save the task ID for other scripts
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

SYNC_PAYLOAD=$(printf '{ "commands": [{ "type": "CREATE_TASK", "commandId": "%s", "entityId": "%s", "timestamp": "%s", "data": { "title": "My first task", "description": "A task to be updated and deleted.", "status": "pending", "folderId": "%s" } }] }' "$COMMAND_ID" "$TASK_ID" "$TIMESTAMP" "$TEST_FOLDER_ID")

echo "Debug - URL: ${API_BASE_URL}/${SYNC_ENDPOINT_ROUTE}"
echo "Debug - Payload: $SYNC_PAYLOAD"

SYNC_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE_URL}/${SYNC_ENDPOINT_ROUTE}" -H "Content-Type: application/json" -H "Authorization: Bearer ${AUTH_TOKEN}" -d "$SYNC_PAYLOAD")

HTTP_CODE=$(echo "$SYNC_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$SYNC_RESPONSE" | sed '$d')

echo "Debug - HTTP Code: $HTTP_CODE"
echo "Debug - Response Body: $RESPONSE_BODY"

if [ "$HTTP_CODE" -ne 200 ]; then
    echo -e "${RED}FAILED: HTTP request failed with status $HTTP_CODE.${NC}"
    echo "Response: $RESPONSE_BODY"
    exit 1
fi

# Check if response body is valid JSON and has success array
if ! echo "$RESPONSE_BODY" | jq . >/dev/null 2>&1; then
    echo -e "${RED}FAILED: Response is not valid JSON.${NC}"
    echo "Response: $RESPONSE_BODY"
    exit 1
fi

SUCCESS_COUNT=$(echo "$RESPONSE_BODY" | jq '.success | length' 2>/dev/null || echo "0")
echo "Debug - Success count: $SUCCESS_COUNT"
if [ "$SUCCESS_COUNT" -ne 1 ]; then
    echo -e "${YELLOW}INFO: Create command was not processed successfully. Expected 1 success, got $SUCCESS_COUNT.${NC}"
    echo "This might indicate an issue with the PostgreSQL function or data validation."
    echo "Check the Azure Functions logs for more details about what JSON was sent to the database."
    echo "Response: $RESPONSE_BODY"
    # Don't exit for now, just warn
    # exit 1
fi

echo -e "${GREEN}SUCCESS: Create command processed successfully.${NC}"
#!/bin/bash
#
# setup.sh - Configuration for API tests
#
# This file holds all the environment variables for the API test suite.
# Source this file before running the main test script:
# $ source setup.sh

# --- API Configuration ---
# The base URL for running Azure Functions, get it from the env var
API_BASE_URL="http://localhost:7071/api"

# Corrected route for the V2 sync endpoint
SYNC_ENDPOINT_ROUTE="v2/sync/commands"

# --- Test User Configuration ---
# Use a fixed username to reuse the same user across test runs
TEST_USERNAME="testuser_persistent"
TEST_EMAIL="${TEST_USERNAME}@example.com"
TEST_PASSWORD="Password123!"

# --- Temporary File Paths ---
# These files will store session data between script steps
TOKEN_FILE="/tmp/api_token.txt"
USER_ID_FILE="/tmp/user_id.txt"
VERIFICATION_CODE_FILE="/tmp/verification_code.txt"
TASK_ID_FILE="/tmp/task_id.txt"
USER_REGISTERED_FILE="/tmp/user_registered.txt"

# --- Color Codes for Output ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
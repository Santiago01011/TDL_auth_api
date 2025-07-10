#!/bin/bash
#
# test_api.sh - Master test runner for the Serverless Auth API.
# Executes all test scripts in sequence and performs cleanup.

# Ensure we are in the script's directory
cd "$(dirname "$0")"

source ./setup.sh

echo -e "${YELLOW}===== STARTING E2E TEST SUITE =====${NC}"
echo "-------------------------------------"

# Run tests in sequence, exiting on the first failure
./01_create_task.sh && \
./02_update_task.sh && \
./03_delete_task.sh

# Check the exit code of the last command
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ All test scripts passed successfully!${NC}"
else
    echo -e "${RED}❌ One or more test scripts failed.${NC}"
fi

echo "-------------------------------------"
echo -e "${YELLOW}===== E2E TEST SUITE COMPLETE =====${NC}"

# Always run cleanup
./cleanup.sh
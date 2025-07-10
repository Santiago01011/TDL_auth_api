#!/bin/bash
#
# reset_user.sh - Reset the persistent user to force re-registration

source ./setup.sh

echo "Resetting persistent user state..."

# Remove all temporary files
rm -f "$TOKEN_FILE"
rm -f "$USER_ID_FILE" 
rm -f "$VERIFICATION_CODE_FILE"
rm -f "$TASK_ID_FILE"
rm -f "$USER_REGISTERED_FILE"

echo -e "${GREEN}User state reset. Next test run will register a new user.${NC}"

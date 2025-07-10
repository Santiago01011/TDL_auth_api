#!/bin/bash
#
# cleanup.sh - Removes temporary files created by the test suite.

# Load configuration to get file paths
source ./setup.sh

echo "Cleaning up temporary test files..."

# Add TASK_ID_FILE to the list of files to remove
for file in "$TOKEN_FILE" "$USER_ID_FILE" "$VERIFICATION_CODE_FILE" "$TASK_ID_FILE"; do
    if [ -f "$file" ]; then
        rm "$file"
        echo "Removed $(basename "$file")."
    fi
done

echo "Cleanup complete."
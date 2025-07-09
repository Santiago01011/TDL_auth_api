# V2 Command Sync API

## Overview
The V2 Command Sync API provides a secure endpoint for synchronizing command-driven changes from client applications to the server. This endpoint is part of Phase 2B and enables seamless multi-device data synchronization.

## Endpoint Details

**URL**: `POST /api/v2/sync/commands`  
**Authentication**: Bearer JWT Token required  
**Content-Type**: `application/json`

## Request Format

```json
{
  "commands": [
    {
      "action": "create|update|delete",
      "entityType": "task|category|etc",
      "entityId": "unique-identifier",
      "data": {
        // Entity-specific data
      },
      "clientTimestamp": "2023-01-01T00:00:00Z"
    }
  ]
}
```

### Request Fields

- **commands** (required): Array of command objects
- **action** (required): One of "create", "update", or "delete"
- **entityType** (required): Type of entity being modified (e.g., "task")
- **entityId** (required): Unique identifier for the entity
- **data** (optional): Entity-specific data for the operation
- **clientTimestamp** (optional): Timestamp when command was created on client

## Response Format

```json
{
  "success": [
    // Successfully processed commands
  ],
  "conflicts": [
    // Commands that had conflicts (resolved according to merge strategy)
  ],
  "failed": [
    // Commands that failed to process
  ]
}
```

## Authentication

Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

The API will extract the user ID from the JWT token to ensure users can only sync their own data.

## Error Responses

- **400 Bad Request**: Invalid request format or validation errors
- **401 Unauthorized**: Missing, invalid, or expired JWT token
- **500 Internal Server Error**: Database or server errors

### Example Error Response

```json
{
  "message": "Validation errors: Command 1: Action is required, Command 2: Invalid action: invalid_action. Must be one of: create, update, delete"
}
```

## Example Usage

### cURL Example

```bash
curl -X POST https://your-function-app.azurewebsites.net/api/v2/sync/commands \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "commands": [
      {
        "action": "create",
        "entityType": "task",
        "entityId": "task-123",
        "data": {
          "title": "New Task",
          "description": "Task description",
          "completed": false
        },
        "clientTimestamp": "2023-01-01T12:00:00Z"
      }
    ]
  }'
```

### JavaScript Example

```javascript
const response = await fetch('/api/v2/sync/commands', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    commands: [
      {
        action: 'update',
        entityType: 'task',
        entityId: 'task-456',
        data: {
          completed: true
        },
        clientTimestamp: new Date().toISOString()
      }
    ]
  })
});

const result = await response.json();
console.log('Sync result:', result);
```

## Database Integration

The API calls the `todo.merge_task_commands()` PostgreSQL function, which handles:
- Conflict resolution
- Data validation
- Atomic transactions
- Response generation

## Security Features

- JWT token validation
- User isolation (users can only sync their own data)
- Token expiration checks
- Input validation and sanitization
- Secure database operations

## Implementation Notes

- Built with Java 21 and Spring Boot 3
- Deployed as Azure Functions
- Uses Jackson for JSON serialization/deserialization
- Comprehensive error handling and logging
- Follows RESTful API conventions
# V2 Command Sync API

## Overview
The V2 Command Sync API provides a secure endpoint for synchronizing command-driven changes from client applications to the server. This endpoint is part of Phase 2B and enables seamless multi-device data synchronization.

## Endpoint Details

**URL**: `POST /api/v2/sync/commands`  
**Authentication**: Bearer JWT Token required  
**Content-Type**: `application/json`

## Request Format

The API supports two command formats for backward compatibility:

### Legacy Format (v1)
```json
{
  "commands": [
    {
      "action": "create|update|delete",
      "entityType": "task",
      "entityId": "unique-identifier",
      "data": {
        // Entity-specific data
      },
      "clientTimestamp": "2023-01-01T00:00:00Z"
    }
  ]
}
```

### New Format (v2) - Recommended
```json
{
  "commands": [
    {
      "type": "CREATE_TASK|UPDATE_TASK|DELETE_TASK",
      "commandId": "unique-command-identifier",
      "entityId": "unique-entity-identifier",
      "data": {
        // Entity-specific data for CREATE_TASK
        "title": "Task title",
        "description": "Task description",
        "status": "pending|in_progress|completed",
        "folderId": "folder-uuid",
        "dueDate": "2023-01-01T00:00:00Z"
      },
      "timestamp": "2023-01-01T00:00:00Z"
    }
  ]
}
```

### Field Descriptions

#### Legacy Format Fields:
- **commands** (required): Array of command objects
- **action** (required): One of "create", "update", or "delete"
- **entityType** (required): Type of entity being modified (e.g., "task")
- **entityId** (required): Unique identifier for the entity
- **data** (optional): Entity-specific data for the operation
- **clientTimestamp** (optional): Timestamp when command was created on client

#### New Format Fields:
- **commands** (required): Array of command objects
- **type** (required): One of "CREATE_TASK", "UPDATE_TASK", or "DELETE_TASK"
- **commandId** (required): Unique identifier for the command operation
- **entityId** (required): Unique identifier for the entity
- **data** (optional): Entity-specific data for the operation
- **timestamp** (required): Timestamp when command was created on client

#### Update Commands Special Fields:
For UPDATE_TASK commands, use `changedFields` instead of `data`:
```json
{
  "type": "UPDATE_TASK",
  "commandId": "uuid",
  "entityId": "task-uuid",
  "changedFields": {
    "title": "Updated title",
    "status": "completed"
  },
  "timestamp": "2023-01-01T00:00:00Z"
}
```

## Response Format

```json
{
  "success": [
    {
      "type": "CREATE_TASK",
      "entityId": "task-uuid",
      "commandId": "command-uuid"
    }
  ],
  "conflicts": [
    {
      "commandId": "command-uuid",
      "entityId": "task-uuid", 
      "field": "title",
      "clientValue": "Client Title",
      "serverTimestamp": "2023-01-01T00:00:00Z",
      "clientTimestamp": "2023-01-01T00:00:00Z"
    }
  ],
  "failed": [
    {
      "commandId": "command-uuid",
      "error": "Task not found or not accessible",
      "entityId": "task-uuid"
    }
  ]
}
```

### Response Fields

- **success**: Array of successfully processed commands
- **conflicts**: Array of commands that had conflicts during processing (resolved according to merge strategy)
- **failed**: Array of commands that failed to process with error details

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
  "message": "Validation errors: Command 1: Action or type is required, Command 2: Invalid action/type: invalid_action. Must be one of: create, update, delete (or CREATE_TASK, UPDATE_TASK, DELETE_TASK)"
}
```

## Example Usage

### CURL Example (New Format)

```bash
curl -X POST http://localhost:7071/api/v2/sync/commands \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "commands": [
      {
        "type": "CREATE_TASK",
        "commandId": "550e8400-e29b-41d4-a716-446655440000",
        "entityId": "task-123",
        "data": {
          "title": "My First Task",
          "description": "A task created via API",
          "status": "pending",
          "folderId": "folder-uuid",
          "dueDate": "2025-12-31T23:59:59Z"
        },
        "timestamp": "2025-07-10T03:14:04Z"
      }
    ]
  }'
```

### CURL Example (Legacy Format)

```bash
curl -X POST http://localhost:7071/api/v2/sync/commands \
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
          "status": "pending"
        },
        "clientTimestamp": "2023-01-01T12:00:00Z"
      }
    ]
  }'
```

### JavaScript Example (New Format)

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
        type: 'UPDATE_TASK',
        commandId: crypto.randomUUID(),
        entityId: 'task-456',
        changedFields: {
          status: 'completed',
          title: 'Updated Task Title'
        },
        timestamp: new Date().toISOString()
      }
    ]
  })
});

const result = await response.json();
console.log('Sync result:', result);
// Expected output:
// {
//   "success": [{"type": "UPDATE_TASK", "entityId": "task-456", "commandId": "..."}],
//   "conflicts": [],
//   "failed": []
// }
```

### JavaScript Example (Legacy Format)

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

- **Conflict Resolution**: Field-level conflict detection and merging
- **Data Validation**: Ensures data integrity and business rules
- **Atomic Transactions**: All commands in a batch are processed atomically
- **Response Generation**: Returns detailed success, conflict, and failure information
- **Audit Logging**: Commands are logged in `todo.command_log` for audit trails
- **Soft Deletion**: Tasks are soft-deleted with `deleted_at` timestamps
- **Folder Security**: Users can only access tasks in their accessible folders

### Database Schema Integration

The function interacts with these key tables:
- `todo.tasks`: Main task storage with field versioning
- `todo.command_log`: Audit trail of all sync operations  
- `todo.users`: User authentication and authorization
- `todo.folders`: Folder-based task organization
- `todo.get_accessible_folders()`: Function for folder access control

### Field-Level Conflict Resolution

The system supports field-level merging for UPDATE_TASK commands:
- Each field has its own timestamp (`field_versions`)
- Conflicts are detected when server has newer field version
- Client can choose to accept server version or force overwrite
- Conflict details are returned in the response

## Security Features

- JWT token validation
- User isolation (users can only sync their own data)
- Token expiration checks
- Input validation and sanitization
- Secure database operations

## Implementation Notes

- **Technology Stack**: Built with Java 21 and Spring Boot 3
- **Deployment**: Deployed as Azure Functions with serverless architecture
- **JSON Processing**: Uses Jackson for serialization/deserialization with unknown property tolerance
- **Error Handling**: Comprehensive error handling with detailed logging and user-friendly messages
- **API Design**: Follows RESTful conventions with proper HTTP status codes
- **Backward Compatibility**: Supports both legacy (v1) and new (v2) command formats
- **Validation**: Multi-layer validation (input validation, business rules, database constraints)
- **Logging**: Structured logging with correlation IDs for debugging and monitoring
- **Configuration**: Environment-based configuration for different deployment environments

### Code Quality Features

- **Unit Tests**: Comprehensive test coverage for all service layers
- **Integration Tests**: End-to-end testing with real database operations
- **Error Recovery**: Graceful handling of database failures and network issues
- **Security**: Input sanitization, SQL injection prevention, and user isolation
- **Performance**: Optimized database queries and efficient JSON processing

## Testing

The API includes comprehensive end-to-end tests located in the `scripts/` directory:

### Test Scripts

- **`01_create_task.sh`**: Tests user registration, authentication, and task creation
- **`02_update_task.sh`**: Tests task updates with field-level conflict resolution
- **`03_delete_task.sh`**: Tests soft deletion of tasks
- **`test_api.sh`**: Master test runner that executes all tests in sequence
- **`setup.sh`**: Configuration and environment variables for tests
- **`cleanup.sh`**: Cleans up temporary test files
- **`reset_user.sh`**: Resets user state for fresh testing

### Running Tests

```bash
# Run all tests
./test_api.sh

# Run individual tests
./01_create_task.sh
./02_update_task.sh
./03_delete_task.sh

# Reset user state
./reset_user.sh
```

### Test Coverage

The tests validate:
- ✅ User registration and verification
- ✅ JWT authentication and token validation
- ✅ Folder access and permissions
- ✅ Task creation with proper data validation
- ✅ Task updates with conflict resolution
- ✅ Task soft deletion
- ✅ Database integrity and timestamps
- ✅ Error handling and validation messages
- ✅ Both legacy and new command formats

## Performance and Limitations

- **Command Batch Size**: Recommended maximum of 100 commands per request
- **Request Timeout**: 30 seconds for Azure Functions
- **Database Transactions**: All commands in a batch are processed atomically
- **Conflict Resolution**: Uses field-level merging with last-write-wins strategy
- **Rate Limiting**: Protected by JWT token validation and user isolation
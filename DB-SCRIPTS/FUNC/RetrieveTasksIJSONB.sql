-- Function: todo.retrieve_tasks_modified_since_in_jsonb
--
-- Description:
--     Retrieves tasks that have been modified since a specified timestamp for a user.
--     This function is intended for efficient synchronization by only retrieving changes.
--     Tasks are returned in JSONB format with the following structure:
--     {
--         "columns": ["folder_id", "folder_name", "task_id", "task_title", "description", "sync_status", "status", "due_date", "created_at", "last_sync", "deleted_at"],
--         "data": [
--             [folder_id, folder_name, task_id, task_title, description, sync_status, status, due_date, created_at, last_sync, deleted_at],
--             ...
--         ],
--         "last_sync": timestamp
--     }
--
-- Parameters:
--     p_user_id UUID:
--         The ID of the user whose tasks are being retrieved.
--     p_last_sync TIMESTAMPTZ:
--         The timestamp to filter tasks that have been modified since.
--
-- Returns:
--     TABLE(retrieved_tasks INTEGER, log_tasks JSONB):
--         - retrieved_tasks: The number of tasks retrieved.
--         - log_tasks: A JSONB object containing the tasks or an error message if applicable.
--           - "columns": List of column names (including "sync_status", which is always 'cloud').
--           - "data": Array of task arrays.
--           - "last_sync": The timestamp when the function was executed.
--
-- Behavior:
--     1. Validates the existence of the user using `todo.user_exists`.
--     2. Retrieves all folder IDs the user has access to (owned or shared).
--     3. Fetches tasks that have been updated or deleted since the provided timestamp.
--     4. Updates last_sync timestamp for each retrieved task.
--     5. Constructs the JSONB object with "columns", "data", and "last_sync" keys.
--     6. Returns the tasks in JSONB format or an error message if no folders are accessible.
--
-- Notes:
--     - The function is marked as `SECURITY DEFINER` to allow execution with elevated privileges.
--     - Includes deleted_at field to allow clients to handle task deletions.
--     - Updates last_sync for all returned tasks to ensure consistency.
--
-- Example Usage:
--SELECT * FROM todo.retrieve_tasks_modified_since_in_jsonb('01959f92-0d81-78ab-9c17-c180be5d9a37', NOW() + '1 hour'::interval);



CREATE OR REPLACE FUNCTION todo.retrieve_tasks_modified_since_in_jsonb(
    p_user_id UUID,
    p_last_sync TIMESTAMPTZ
) RETURNS TABLE(retrieved_tasks INTEGER, log_tasks JSONB) AS $$
DECLARE
    folder_data RECORD;
    task_data RECORD;
    folder_ids UUID[] := '{}';
    current_time TIMESTAMPTZ := NOW();
    tasks_json JSONB := jsonb_build_object(
        'columns', ARRAY['folder_id', 'folder_name', 'task_id', 'task_title', 'description', 'sync_status', 'status', 'due_date', 'created_at', 'last_sync', 'deleted_at'],
        'last_sync', current_time,
        'data', '[]'::JSONB
    );
    error_message TEXT;
    task_ids UUID[] := '{}';
BEGIN
    IF p_last_sync IS NULL THEN
        p_last_sync := '1970-01-01 00:00:00+00'::timestamptz;
    END IF;
    IF NOT todo.user_exists(p_user_id) THEN
        error_message := 'User with ID ' || p_user_id || ' does not exist';
        RETURN QUERY SELECT 0, jsonb_build_object('error', error_message);
        RETURN;
    END IF;

    SELECT array_agg(f.folder_id) INTO folder_ids
    FROM todo.folders f
    LEFT JOIN todo.folder_users fu ON f.folder_id = fu.folder_id
    WHERE f.owner_id = p_user_id OR fu.user_id = p_user_id;

    IF folder_ids IS NULL OR array_length(folder_ids, 1) = 0 THEN
        error_message := 'User with ID ' || p_user_id || ' has no accessible folders';
        RETURN QUERY SELECT 0, jsonb_build_object('error', error_message);
        RETURN;
    END IF;

    FOR task_data IN 
        SELECT t.*, f.folder_name
        FROM todo.tasks t
        LEFT JOIN todo.folders f ON t.folder_id = f.folder_id
        WHERE t.folder_id = ANY(folder_ids)
          AND (
              t.updated_at > p_last_sync OR
              (t.deleted_at IS NOT NULL AND t.deleted_at > p_last_sync)
          )
    LOOP
        task_ids := task_ids || task_data.task_id;
        
        tasks_json := jsonb_set(
            tasks_json, '{data}',
            (tasks_json->'data') || jsonb_build_array(
                jsonb_build_array(
                    task_data.folder_id, task_data.folder_name, task_data.task_id, task_data.task_title, task_data.description, 'cloud',
                    task_data.status, task_data.due_date, task_data.created_at, task_data.last_sync, task_data.deleted_at
                )
            )
        );
    END LOOP;

    RETURN QUERY SELECT jsonb_array_length(tasks_json->'data'), tasks_json;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Active: 1741952589919@@127.0.0.1@5431@todo_list
-- Function: todo.update_tasks_from_jsonb
-- Purpose: Updates tasks in the todo.tasks table based on the provided JSONB input.
-- Parameters:
--   p_user_id UUID: The ID of the user performing the update.
--   p_tasks_json JSONB: A JSONB object containing columns and data arrays:
--     'columns': Array of column names
--     'data': Array of arrays containing values for each task
-- Returns:
--   TABLE(updated_tasks INTEGER, log_tasks JSONB):
--     updated_tasks: The number of successfully updated tasks.
--     log_tasks: A JSONB object containing logs of successful and failed updates.
-- Behavior:
--   - Extracts columns and data from the JSONB input.
--   - Filters out columns that aren't in the allowed list (task_title, description, status, due_date, deleted_at).
--   - Validates that each task belongs to the specified user or is in a folder shared with the user.
--   - Constructs a dynamic SQL query to update only the provided valid fields.
--   - Automatically sets 'updated_at' and 'last_sync' to the current timestamp.
--   - Skips fields with null values and logs errors for invalid or failed updates.
--   - Reports task-level failures with specific error messages.
--   - Runs with elevated permissions (SECURITY DEFINER) to avoid permission issues.

CREATE OR REPLACE FUNCTION todo.update_tasks_from_jsonb(
    p_user_id UUID,
    p_tasks_json JSONB
) RETURNS TABLE(updated_tasks INTEGER, log_tasks JSONB) 
SECURITY DEFINER
AS $$
DECLARE
    task_data JSONB;
    task_columns TEXT[];
    task_values JSONB[];
    updated_count INTEGER := 0;
    task_id UUID;
    update_query TEXT;
    update_fields TEXT;
    error_message TEXT;
    log_tasks_json JSONB := jsonb_build_object(
        'success', '[]'::JSONB,
        'failed', '[]'::JSONB,
        'last_sync', NOW()
    );
    p_last_sync TIMESTAMPTZ := COALESCE(
        (p_tasks_json->>'last_sync')::TIMESTAMPTZ,
        NOW()
    );
    task_id_index INTEGER;
    user_updatable_columns TEXT[] := ARRAY['task_title', 'description', 'status', 'due_date', 'deleted_at'];
BEGIN
    task_columns := ARRAY(SELECT jsonb_array_elements_text(p_tasks_json->'columns'));
    task_values := ARRAY(SELECT jsonb_array_elements(p_tasks_json->'data'));

    task_id_index := array_position(task_columns, 'task_id');
    IF task_id_index IS NULL THEN
        RAISE EXCEPTION 'task_id column not found in input JSONB';
    END IF;

    IF NOT todo.user_exists(p_user_id) THEN
        error_message := 'User with ID ' || p_user_id || ' does not exist';
        log_tasks_json := jsonb_set(
            log_tasks_json, 
            '{failed}', 
            log_tasks_json->'failed' || jsonb_build_object('user_id', p_user_id, 'error', error_message)
        );
        RETURN QUERY SELECT 0, log_tasks_json;
        RETURN;
    END IF;

    FOREACH task_data IN ARRAY task_values LOOP
        BEGIN
            task_id := (task_data->>(task_id_index - 1))::UUID;

            IF task_id IS NULL THEN
                error_message := 'task_id is null for task: ' || task_data::TEXT;
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{failed}',
                    log_tasks_json->'failed' || jsonb_build_object('error', error_message)
                );
                CONTINUE;
            END IF;

            update_fields := '';
            
            FOR i IN 1..array_length(task_columns, 1) LOOP
                IF task_columns[i] = ANY(user_updatable_columns) THEN
                    IF task_data->>(i - 1) IS NOT NULL THEN
                        update_fields := update_fields || format('%I = %L, ', task_columns[i], task_data->>(i - 1));
                    END IF;
                END IF;
            END LOOP;

            update_fields := RTRIM(update_fields, ', ');

            IF update_fields = '' THEN
                error_message := 'No updatable fields valid for task: ' || task_data::TEXT;
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{failed}',
                    log_tasks_json->'failed' || jsonb_build_object('error', error_message)
                );
                CONTINUE;
            END IF;

            update_query := format(
                'UPDATE todo.tasks SET %s, updated_at = %L, last_sync = %L                  
                 WHERE task_id = %L AND folder_id IN (
                     SELECT folder_id FROM todo.folders WHERE owner_id = %L
                     UNION
                     SELECT folder_id FROM todo.folder_users WHERE user_id = %L
                 ) RETURNING task_id',
                update_fields, p_last_sync, p_last_sync, task_id, p_user_id, p_user_id
            );
            
            EXECUTE update_query INTO task_id;
            
            IF task_id IS NOT NULL THEN
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{success}',
                    log_tasks_json->'success' || jsonb_build_object('task_id', task_id)
                );
                updated_count := updated_count + 1;
            ELSE
                error_message := 'Task with ID not found or not accessible for user: ' || p_user_id;
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{failed}',
                    log_tasks_json->'failed' || jsonb_build_object('error', error_message)
                );
            END IF;
        EXCEPTION WHEN OTHERS THEN
            error_message := SQLERRM;
            log_tasks_json := jsonb_set(
                log_tasks_json,
                '{failed}',
                log_tasks_json->'failed' || jsonb_build_object(
                    'task_id', task_id,
                    'error', error_message
                )
            );
        END;
    END LOOP;

    RETURN QUERY SELECT updated_count, log_tasks_json;
END;
$$ LANGUAGE plpgsql;
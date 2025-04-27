/*
Function: todo.insert_tasks_from_jsonb

Description:
    This function inserts tasks into the `todo.tasks` table from a JSONB input. 
    It validates the user, folder, and task data before insertion. 
    Tasks that fail validation or encounter errors during processing are returned in the `log_tasks` JSONB output.

Parameters:
    p_user_id UUID:
        The ID of the user for whom the tasks are being inserted.
    p_tasks_json JSONB:
        A JSONB object containing an array of task data under the key 'data'. 
        Each task is represented as an array with the following structure:
        [
            folder_id (UUID or NULL),
            ... (unused fields for this function),
            old_task_id (TEXT or NULL),
            task_title (TEXT),
            description (TEXT),
            ... (unused fields for this function),
            status (TEXT),
            due_date (TIMESTAMPTZ or NULL),
            created_at (TIMESTAMPTZ or NULL)
        ]

Behavior:
    1. Validates the existence of the user using `todo.user_exists`.
    2. Retrieves the default folder ID for the user using `todo.get_default_folder`.
    3. Iterates over the tasks in the input JSONB:
        - Validates the task status using `todo.is_valid_task_status`.
        - Determines the folder ID (either provided or default).
        - Validates the folder using `todo.validate_folder`.
        - Generates a unique task ID using `todo.generate_task_id`.
        - Prepares the task data for batch insertion.
        - Handles any errors during processing and logs them in the `failed` key of `log_tasks` JSONB.
    4. Performs a batch insert of all valid tasks into the `todo.tasks` table.
    5. Returns the count of inserted tasks and the `log_tasks` JSONB.

Returns:
    TABLE(inserted_tasks INTEGER, log_tasks JSONB):
        - inserted_tasks: The number of successfully inserted tasks.
        - log_tasks: A JSONB object containing two keys:
          - `success`: A JSONB array of successfully processed tasks with their old and new task IDs.
          - `failed`: A JSONB array containing details of tasks that failed validation or insertion, 
            along with error messages.

Notes:
    - The function uses `jsonb_array_elements` to parse the input JSONB array.
    - Errors are captured and logged for each task individually, allowing partial success.
    - The function is marked as `SECURITY DEFINER` to allow execution with elevated privileges.

Example Usage:
    SELECT * FROM todo.insert_tasks_from_jsonb(
        '123e4567-e89b-12d3-a456-426614174000',
        '{"data": [
            [null, null, "old_task_1", "Task 1", "Description 1", null, "pending", null, null],
            [null, null, "old_task_2", "Task 2", "Description 2", null, "invalid_status", null, null]
        ]}'::JSONB
    );
*/

-- SELECT * FROM todo.insert_tasks_from_jsonb(
--         '123e4567-e89b-12d3-a456-426614174000',
--         '{"data": [
--             [null, null, "old_task_1", "Task 1", "Description 1", null, "pending", null, null],
--             [null, null, "old_task_2", "Task 2", "Description 2", null, "invalid_status", null, null]
--         ]}'::JSONB
--     );
-- SELECT * FROM todo.insert_tasks_from_jsonb(
--     '01959f92-0d81-78ab-9c17-c180be5d9a37',
--   '{"data" : [ 
--         [ null, null, "01962737-157e-746b-bf30-cf7fb868e621", "Test Task", "This is a test task", "cloud", "prending", null, "2007-12-03T10:15:30+01:00" ], 
--         [
--             "01959f92-f4a8-797f-8917-a1023364bc4d",
--             "test_folder",
--             "0196496d-114b-7ca4-9318-f8fa22855d90",
--             "Test Task",
--             "This is a test task to check permissions",
--             "cloud",
--             "2025-04-18T15:05:25.735954",
--             "pending",
--             "2025-03-20T10:00",
--             "2025-04-18T15:03:26.795775"
--         ],
--         [ null, null, "old_task_2", "Task 2", "Description 2", null, "invalid_status", null, null ] ],
--     "last_sync": "2025-04-16T20:22:23.582849700-03:00"
--     }'::jsonb
-- );

CREATE OR REPLACE FUNCTION todo.insert_tasks_from_jsonb(
    p_user_id UUID,
    p_tasks_json JSONB
) RETURNS TABLE(inserted_tasks INTEGER, log_tasks JSONB) AS $$
DECLARE
    v_default_folder_id UUID;
    task_data RECORD;
    inserted_count INTEGER := 0;
    v_folder_id UUID;
    v_task_id UUID;
    v_created_at TIMESTAMPTZ;
    values_list TEXT := '';
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
BEGIN
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
    
    v_default_folder_id := todo.get_default_folder(p_user_id);

    FOR task_data IN 
        SELECT 
            (elem->>0)::UUID AS folder_id,
            elem->>3 AS task_title,
            elem->>4 AS description,
            elem->>7 AS status,
            CASE 
                WHEN elem->>8 IS NOT NULL THEN (elem->>8)::TIMESTAMPTZ 
                ELSE NULL 
            END AS due_date,
            CASE 
                WHEN elem->>9 IS NOT NULL THEN (elem->>9)::TIMESTAMPTZ 
                ELSE NOW() 
            END AS created_at,
            elem->>2 AS old_task_id
        FROM jsonb_array_elements(p_tasks_json->'data') AS elem
    LOOP
        BEGIN
            IF NOT todo.is_valid_task_status(task_data.status) THEN
                error_message := 'Invalid status value: ' || task_data.status;
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{failed}',
                    log_tasks_json->'failed' || jsonb_build_object(
                        'task', 
                        jsonb_build_array( 
                            task_data.folder_id, task_data.task_title, task_data.description, task_data.status, task_data.due_date, task_data.created_at
                        ),
                        'error', error_message
                    )
                );
                CONTINUE;
            END IF;
            
            IF task_data.folder_id IS NOT NULL THEN
                v_folder_id := task_data.folder_id;
            ELSE
                v_folder_id := v_default_folder_id;
            END IF;
            
            IF NOT todo.validate_folder(v_folder_id, p_user_id) THEN
                error_message := 'Invalid or inaccessible folder_id';
                log_tasks_json := jsonb_set(
                    log_tasks_json,
                    '{failed}',
                    log_tasks_json->'failed' || jsonb_build_object(
                        'task',
                        jsonb_build_array( 
                            task_data.folder_id, task_data.task_title, task_data.description, task_data.status, task_data.due_date, task_data.created_at 
                        ),
                        'error', error_message
                    )
                );
                CONTINUE;
            END IF;
            
            v_created_at := COALESCE(task_data.created_at, NOW());
            v_task_id := todo.generate_task_id(v_created_at);
            
            values_list := values_list || format(
                '(%L, %L, %L, %L, %L, %L, %L, %L, %L),',
                v_task_id, v_folder_id, task_data.task_title, task_data.description, task_data.status, task_data.due_date, 
                v_created_at, p_last_sync, p_last_sync
            );
            
            log_tasks_json := jsonb_set(
                log_tasks_json,
                '{success}',
                log_tasks_json->'success' || jsonb_build_object(
                    'old', task_data.old_task_id,
                    'new', v_task_id
                )
            );
            
            inserted_count := inserted_count + 1;
        EXCEPTION WHEN OTHERS THEN
            error_message := SQLERRM;
            log_tasks_json := jsonb_set(
                log_tasks_json,
                '{failed}',
                log_tasks_json->'failed' || jsonb_build_object(
                    'task', 
                    jsonb_build_array( 
                        task_data.folder_id, task_data.task_title, task_data.description, task_data.status, task_data.due_date, task_data.created_at
                    ),
                    'error', error_message
                )
            );
        END;
    END LOOP;
    
    IF values_list <> '' THEN
        values_list := LEFT(values_list, LENGTH(values_list) - 1);
        EXECUTE 'INSERT INTO todo.tasks (
            task_id, folder_id, task_title, description, status, due_date, 
            created_at, last_sync, updated_at
        ) VALUES ' || values_list;
    END IF;
    
    RETURN QUERY SELECT inserted_count, log_tasks_json;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Active: 1741952589919@@127.0.0.1@5431@todo_list
--------------------------------Helper Functions--------------------------------
-- This script contains helper functions for the To-Do List application database.
-- 
-- Functions:
-- 1. user_exists(p_user_id UUID): Checks if a user exists in the database.
-- 2. get_default_folder(p_user_id UUID): Retrieves the default folder ID for a given user.
-- 3. validate_folder(p_folder_id UUID, p_user_id UUID): Validates if a user has access to a specific folder.
-- 4. generate_task_id(p_created_at TIMESTAMPTZ): Generates a task ID based on a timestamp.
-- 5. is_valid_task_status(p_status TEXT): Validates if a task status is valid.

CREATE OR REPLACE FUNCTION todo.user_exists(p_user_id UUID) 
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(SELECT 1 FROM todo.users WHERE user_id = p_user_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION todo.get_default_folder(p_user_id UUID) 
RETURNS UUID AS $$
DECLARE
    v_default_folder_id UUID;
BEGIN
    SELECT folder_id INTO v_default_folder_id
    FROM todo.folders
    WHERE owner_id = p_user_id AND folder_name = 'Default Folder'
    LIMIT 1;    
    RETURN v_default_folder_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION todo.validate_folder(p_folder_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN p_folder_id IS NOT NULL 
           AND EXISTS(
               SELECT 1 
               FROM todo.folders f
               LEFT JOIN todo.folder_users fu ON f.folder_id = fu.folder_id
               WHERE f.folder_id = p_folder_id 
                 AND (f.owner_id = p_user_id OR fu.user_id = p_user_id)
           );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION todo.generate_task_id(p_created_at TIMESTAMPTZ)
RETURNS UUID AS $$
BEGIN
    RETURN todo.uuid_timestamptz_to_v7(p_created_at AT TIME ZONE 'UTC');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION todo.is_valid_task_status(p_status TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN p_status IN ('pending', 'in_progress', 'completed');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

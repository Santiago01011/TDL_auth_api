
CREATE SCHEMA IF NOT EXISTS todo;

CREATE EXTENSION IF NOT EXISTS pg_uuidv7 SCHEMA todo;

------------------------------------------------ TASK_MANAGER ROLE ------------------------------------------------
CREATE OR REPLACE FUNCTION create_task_manager_role_if_not_exists() RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'task_manager') THEN
        EXECUTE 'CREATE ROLE task_manager LOGIN PASSWORD ''task_manager_password''';
        RAISE NOTICE 'Role "task_manager" created.';
    ELSE
        RAISE NOTICE 'Role "task_manager" already exists.';
    END IF;
    EXECUTE 'GRANT CONNECT ON DATABASE todo_list TO task_manager';
    EXECUTE 'GRANT USAGE ON SCHEMA todo TO task_manager';
    EXECUTE 'GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA todo TO task_manager';
    EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA todo GRANT EXECUTE ON FUNCTIONS TO task_manager';
END;
$$ LANGUAGE plpgsql;
----------------------------------------------------- API ROLE -----------------------------------------------------
CREATE OR REPLACE FUNCTION create_api_role_if_not_exists() RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'api_role') THEN
        EXECUTE 'CREATE ROLE api_role LOGIN PASSWORD ''api_role_password''';
        RAISE NOTICE 'Role "api_role" created.';
    ELSE
        RAISE NOTICE 'Role "api_role" already exists.';
    END IF;
    EXECUTE 'GRANT CONNECT ON DATABASE todo_list TO api_role';
    EXECUTE 'GRANT USAGE ON SCHEMA todo TO api_role';
    EXECUTE 'GRANT SELECT, INSERT, UPDATE ON todo.users TO api_role';
    EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON todo.pending_users TO api_role';

END;
$$ LANGUAGE plpgsql;
----------------------------------------------------- Tables -----------------------------------------------------
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_status' AND typnamespace = 'todo'::regnamespace) THEN
        CREATE TYPE todo.task_status AS ENUM ('pending', 'in_progress', 'completed');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS todo.users (
    user_id UUID PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    UNIQUE (email, username)
);

CREATE TABLE todo.pending_users (
    pending_id UUID PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    verification_code TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS todo.folders (
    folder_id UUID PRIMARY KEY,
    folder_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    last_sync TIMESTAMPTZ,
    owner_id UUID NOT NULL REFERENCES todo.users(user_id)
);

CREATE TABLE IF NOT EXISTS todo.folder_users (
    folder_id UUID NOT NULL REFERENCES todo.folders(folder_id),
    user_id UUID NOT NULL REFERENCES todo.users(user_id),
    PRIMARY KEY (folder_id, user_id)
);

CREATE TABLE IF NOT EXISTS todo.tasks (
    task_id UUID PRIMARY KEY,
    task_title TEXT NOT NULL,
    description TEXT,
    status todo.task_status NOT NULL,
    due_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    last_sync TIMESTAMPTZ,
    folder_id UUID NOT NULL REFERENCES todo.folders(folder_id)
);
-------------------------------------------- Default Folders Function --------------------------------------------
CREATE OR REPLACE FUNCTION public.create_user_default_folder()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO todo.folders (folder_id, folder_name, created_at, owner_id)
    VALUES (todo.uuid_generate_v7(), 'Default Folder', NOW(), NEW.user_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-------------------------------------------- Default Folders Trigger ---------------------------------------------
CREATE OR REPLACE TRIGGER create_default_folders_trigger
AFTER INSERT ON todo.users
FOR EACH ROW
EXECUTE FUNCTION public.create_user_default_folder();
------------------------------------------- Call create role functions -------------------------------------------
SELECT create_api_role_if_not_exists();
SELECT create_task_manager_role_if_not_exists();

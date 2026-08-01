CREATE TABLE IF NOT EXISTS qaip_projects (
    project_id text PRIMARY KEY,
    project_payload jsonb NOT NULL
);

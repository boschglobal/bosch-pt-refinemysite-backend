ALTER TABLE projects
    ADD COLUMN created_by TEXT NOT NULL DEFAULT '<unknown>';

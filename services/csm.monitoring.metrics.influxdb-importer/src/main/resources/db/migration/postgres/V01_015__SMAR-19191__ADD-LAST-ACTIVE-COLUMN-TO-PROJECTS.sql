-- Track when a Project was last active to get a more useful Project count for the platform

ALTER TABLE projects
    ADD COLUMN last_active TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX "identifier_last_active_desc_idx" ON projects (identifier, last_active DESC);

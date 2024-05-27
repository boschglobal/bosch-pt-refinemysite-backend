-- Add a table containing data about BIM models, so we can track BIM adoption.

CREATE TABLE bim_models
(
    project_id  TEXT NOT NULL,
    model_id    TEXT NOT NULL,
    version_id  TEXT NOT NULL,
    model_name  TEXT NOT NULL DEFAULT '<unknown>',
    uploaded_by TEXT NOT NULL DEFAULT '<unknown>',
    status      TEXT NOT NULL DEFAULT '<unknown>',
    PRIMARY KEY (project_id, model_id, version_id)
);

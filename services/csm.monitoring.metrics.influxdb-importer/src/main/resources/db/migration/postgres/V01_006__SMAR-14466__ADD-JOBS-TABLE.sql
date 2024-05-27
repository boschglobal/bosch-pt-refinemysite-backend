CREATE TABLE jobs
(
    created_at         TIMESTAMP NOT NULL,
    job_identifier     TEXT      NOT NULL,
    job_type           TEXT      NOT NULL,
    status             TEXT      NOT NULL,
    project_identifier TEXT,
    duration_seconds   BIGINT,
    value              SMALLINT  NOT NULL DEFAULT 1,
    UNIQUE (created_at, job_identifier)
);

SELECT * FROM create_hypertable('jobs', 'created_at');
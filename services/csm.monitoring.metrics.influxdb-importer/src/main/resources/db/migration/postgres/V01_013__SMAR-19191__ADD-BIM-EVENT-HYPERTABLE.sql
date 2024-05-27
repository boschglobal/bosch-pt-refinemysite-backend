-- Add table with all BIM related events for time series analysis

CREATE TABLE bim_events
(
    event_time  TIMESTAMP NOT NULL,
    event_type  TEXT      NOT NULL,
    project_id  TEXT      NOT NULL,
    model_id    TEXT,
    version_id  TEXT,
    workarea_id TEXT
);

SELECT * FROM create_hypertable('bim_events', 'event_time');

CREATE INDEX "project_id_event_time_idx" ON bim_events (project_id, event_time DESC);

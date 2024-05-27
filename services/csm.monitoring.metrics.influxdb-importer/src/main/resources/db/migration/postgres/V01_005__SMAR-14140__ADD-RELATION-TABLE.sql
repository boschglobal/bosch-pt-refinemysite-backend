CREATE TABLE relations (
    event_time TIMESTAMP NOT NULL,
    event_name TEXT NOT NULL,
    aggregate_identifier TEXT NOT NULL,
    project_identifier TEXT NOT NULL,
    relation_type TEXT NOT NULL,
    relation_constellation TEXT NOT NULL,
    value SMALLINT NOT NULL DEFAULT 1,
    UNIQUE (event_time, aggregate_Identifier, event_name)
);

SELECT * FROM create_hypertable('relations', 'event_time');
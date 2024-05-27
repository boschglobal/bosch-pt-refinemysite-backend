CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE events (
    event_time TIMESTAMP NOT NULL,
    event_name TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    aggregate_identifier TEXT NOT NULL,
    root_context_identifier TEXT NOT NULL,
    UNIQUE (event_time, aggregate_Identifier, event_name)
);

SELECT * FROM create_hypertable('events', 'event_time');
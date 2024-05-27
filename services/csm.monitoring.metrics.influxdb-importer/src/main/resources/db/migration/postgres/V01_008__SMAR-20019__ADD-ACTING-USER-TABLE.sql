CREATE TABLE acting_users
(
    event_time  TIMESTAMP NOT NULL,
    event_name  TEXT      NOT NULL,
    acting_user TEXT      NOT NULL
);

SELECT * FROM create_hypertable('acting_users', 'event_time');

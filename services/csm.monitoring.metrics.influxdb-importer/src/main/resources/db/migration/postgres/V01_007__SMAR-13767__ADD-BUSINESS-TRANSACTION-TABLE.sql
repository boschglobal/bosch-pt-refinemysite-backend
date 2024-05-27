CREATE TABLE businesstransactions
(
    date                   TIMESTAMP NOT NULL,
    transaction_identifier TEXT      NOT NULl,
    project_identifier     TEXT      NOT NULl,
    type                   TEXT      NOT NULL,
    duration_seconds       BIGINT,
    UNIQUE (date, transaction_identifier)
);

SELECT *
FROM create_hypertable('businesstransactions', 'date');
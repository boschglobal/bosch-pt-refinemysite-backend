CREATE TABLE projects(
    identifier    TEXT NOT NULL,
    name          TEXT NOT NULL,
    UNIQUE (identifier)
);
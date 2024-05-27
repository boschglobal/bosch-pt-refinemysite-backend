ALTER TABLE events
    ADD COLUMN
        context TEXT NOT NULL default 'UNDEFINED';


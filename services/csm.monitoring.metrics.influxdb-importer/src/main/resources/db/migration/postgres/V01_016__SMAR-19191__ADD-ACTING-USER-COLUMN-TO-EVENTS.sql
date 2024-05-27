-- Denormalize from acting_users table, because joining two event hypertables does not perform adequately
-- without additional optimization.  This replaces the last_active column in projects, because most projects
-- are being kept active by the batch jobs performed by the system user.

ALTER TABLE events
    ADD COLUMN acting_user TEXT NOT NULL DEFAULT '<unknown>';

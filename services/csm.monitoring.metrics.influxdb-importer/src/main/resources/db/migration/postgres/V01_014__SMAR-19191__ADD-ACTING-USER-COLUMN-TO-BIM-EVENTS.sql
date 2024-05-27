-- Denormalize from acting_users table, because joining two event hypertables does not perform adequately
-- without additional optimization.  This approach is simpler.

ALTER TABLE bim_events
    ADD COLUMN acting_user TEXT NOT NULL DEFAULT '<unknown>';

CREATE INDEX "acting_user_project_id_event_time_desc_idx" ON bim_events (acting_user, project_id, event_time DESC);

DROP INDEX "project_id_event_time_idx";

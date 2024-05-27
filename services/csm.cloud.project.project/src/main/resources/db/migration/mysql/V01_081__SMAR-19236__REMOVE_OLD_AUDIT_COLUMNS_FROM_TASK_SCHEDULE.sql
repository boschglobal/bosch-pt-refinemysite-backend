DROP TRIGGER set_task_schedule_audit_columns_on_insert;
DROP TRIGGER set_task_schedule_audit_columns_on_update;

ALTER TABLE task_schedule DROP FOREIGN KEY FK_TaskSchedule_CreatedBy;
ALTER TABLE task_schedule DROP FOREIGN KEY FK_TaskSchedule_LastModifiedBy;

ALTER TABLE task_schedule DROP COLUMN created_by_id;
ALTER TABLE task_schedule DROP COLUMN last_modified_by_id;
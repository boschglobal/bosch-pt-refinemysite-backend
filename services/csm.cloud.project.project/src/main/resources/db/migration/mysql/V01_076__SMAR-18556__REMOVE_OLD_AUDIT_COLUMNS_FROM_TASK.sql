DROP TRIGGER set_task_audit_columns_on_insert;
DROP TRIGGER set_task_audit_columns_on_update;

ALTER TABLE task DROP FOREIGN KEY FK_Task_CreatedBy;
ALTER TABLE task DROP FOREIGN KEY FK_Task_LastModifiedBy;

ALTER TABLE task DROP COLUMN created_by_id;
ALTER TABLE task DROP COLUMN last_modified_by_id;
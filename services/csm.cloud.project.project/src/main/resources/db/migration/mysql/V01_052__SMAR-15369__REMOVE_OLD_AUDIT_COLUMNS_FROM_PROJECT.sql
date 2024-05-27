DROP TRIGGER set_project_audit_columns_on_insert;
DROP TRIGGER set_project_audit_columns_on_update;

ALTER TABLE project DROP FOREIGN KEY FK_Project_CreatedBy;
ALTER TABLE project DROP FOREIGN KEY FK_Project_LastModifiedBy;

ALTER TABLE project DROP COLUMN created_by_id;
ALTER TABLE project DROP COLUMN last_modified_by_id;
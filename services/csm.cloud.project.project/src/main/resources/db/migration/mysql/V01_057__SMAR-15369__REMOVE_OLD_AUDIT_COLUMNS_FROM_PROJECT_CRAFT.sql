DROP TRIGGER set_project_craft_audit_columns_on_insert;
DROP TRIGGER set_project_craft_audit_columns_on_update;

ALTER TABLE project_craft DROP FOREIGN KEY FK_ProjCraft_CreatedBy;
ALTER TABLE project_craft DROP FOREIGN KEY FK_ProjCraft_LastModifiedBy;

ALTER TABLE project_craft DROP COLUMN created_by_id;
ALTER TABLE project_craft DROP COLUMN last_modified_by_id;
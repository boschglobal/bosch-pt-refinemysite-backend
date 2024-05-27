DROP TRIGGER set_work_area_list_audit_columns_on_insert;
DROP TRIGGER set_work_area_list_audit_columns_on_update;

ALTER TABLE work_area_list DROP FOREIGN KEY FK_WorkAreaList_CreatedBy;
ALTER TABLE work_area_list DROP FOREIGN KEY FK_WorkAreaList_LastModifiedBy;

ALTER TABLE work_area_list DROP COLUMN created_by_id;
ALTER TABLE work_area_list DROP COLUMN last_modified_by_id;

DROP TRIGGER set_work_area_audit_columns_on_insert;
DROP TRIGGER set_work_area_audit_columns_on_update;

ALTER TABLE work_area DROP FOREIGN KEY FK_WorkArea_CreatedBy;
ALTER TABLE work_area DROP FOREIGN KEY FK_WorkArea_LastModifiedBy;

ALTER TABLE work_area DROP COLUMN created_by_id;
ALTER TABLE work_area DROP COLUMN last_modified_by_id;
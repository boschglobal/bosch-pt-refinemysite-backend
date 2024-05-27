DROP TRIGGER set_milestone_list_audit_columns_on_insert;
DROP TRIGGER set_milestone_list_audit_columns_on_update;

ALTER TABLE milestone_list DROP FOREIGN KEY FK_MilestoneList_CreatedBy;
ALTER TABLE milestone_list DROP FOREIGN KEY FK_MilestoneList_LastModifiedBy;

ALTER TABLE milestone_list DROP COLUMN created_by_id;
ALTER TABLE milestone_list DROP COLUMN last_modified_by_id;

DROP TRIGGER set_milestone_audit_columns_on_insert;
DROP TRIGGER set_milestone_audit_columns_on_update;

ALTER TABLE milestone DROP FOREIGN KEY FK_Milestone_CreatedBy;
ALTER TABLE milestone DROP FOREIGN KEY FK_Milestone_LastModifiedBy;

ALTER TABLE milestone DROP COLUMN created_by_id;
ALTER TABLE milestone DROP COLUMN last_modified_by_id;

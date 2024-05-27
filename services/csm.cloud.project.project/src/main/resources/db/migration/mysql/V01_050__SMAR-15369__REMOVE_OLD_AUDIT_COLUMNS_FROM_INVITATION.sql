DROP TRIGGER set_invitation_audit_columns_on_insert;
DROP TRIGGER set_invitation_audit_columns_on_update;

ALTER TABLE invitation DROP FOREIGN KEY FK_Invitation_CreatedBy;
ALTER TABLE invitation DROP FOREIGN KEY FK_Invitation_LastModifiedBy;

ALTER TABLE invitation DROP COLUMN created_by_id;
ALTER TABLE invitation DROP COLUMN last_modified_by_id;
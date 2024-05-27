DROP TRIGGER set_message_audit_columns_on_insert;
DROP TRIGGER set_message_audit_columns_on_update;

ALTER TABLE message DROP FOREIGN KEY FK_Message_CreatedBy;
ALTER TABLE message DROP FOREIGN KEY FK_Message_LastModifiedBy;

ALTER TABLE message DROP COLUMN created_by_id;
ALTER TABLE message DROP COLUMN last_modified_by_id;
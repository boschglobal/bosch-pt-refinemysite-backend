DROP TRIGGER set_topic_audit_columns_on_insert;
DROP TRIGGER set_topic_audit_columns_on_update;

ALTER TABLE topic DROP FOREIGN KEY FK_Topic_CreatedBy;
ALTER TABLE topic DROP FOREIGN KEY FK_Topic_LastModifiedBy;

ALTER TABLE topic DROP COLUMN created_by_id;
ALTER TABLE topic DROP COLUMN last_modified_by_id;
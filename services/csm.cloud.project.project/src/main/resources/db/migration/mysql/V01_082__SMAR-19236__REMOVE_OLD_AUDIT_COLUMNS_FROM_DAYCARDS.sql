DROP TRIGGER set_day_card_audit_columns_on_insert;
DROP TRIGGER set_day_card_audit_columns_on_update;

ALTER TABLE day_card DROP FOREIGN KEY FK_DayCard_CreatedBy;
ALTER TABLE day_card DROP FOREIGN KEY FK_DayCard_LastModifiedBy;

ALTER TABLE day_card DROP COLUMN created_by_id;
ALTER TABLE day_card DROP COLUMN last_modified_by_id;
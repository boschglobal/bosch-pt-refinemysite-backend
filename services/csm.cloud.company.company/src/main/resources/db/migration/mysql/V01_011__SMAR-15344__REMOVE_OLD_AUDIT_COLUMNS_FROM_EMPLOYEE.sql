alter table employee drop foreign key FK_Employee_CreatedBy;
alter table employee drop foreign key FK_Employee_LastModifiedBy;

alter table employee drop column created_by_id;
alter table employee drop column last_modified_by_id;

drop trigger set_employee_audit_columns_on_insert;
drop trigger set_employee_audit_columns_on_update;

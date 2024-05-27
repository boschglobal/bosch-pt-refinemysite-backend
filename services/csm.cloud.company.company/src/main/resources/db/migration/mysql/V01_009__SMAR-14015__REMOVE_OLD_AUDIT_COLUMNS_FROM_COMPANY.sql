alter table company drop foreign key FK_Company_CreatedBy;
alter table company drop foreign key FK_Company_LastModifiedBy;

alter table company drop column created_by_id;
alter table company drop column last_modified_by_id;

drop trigger set_company_audit_columns_on_insert;
drop trigger set_company_audit_columns_on_update;

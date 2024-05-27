alter table employee drop foreign key FK_Employee_User;
alter table employee drop column user_id;
drop trigger set_employee_user_ref_on_insert;

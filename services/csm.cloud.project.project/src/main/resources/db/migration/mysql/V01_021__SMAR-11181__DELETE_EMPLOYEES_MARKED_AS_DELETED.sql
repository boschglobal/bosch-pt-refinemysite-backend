delete from employee_role
    where employee_id in
      (Select id from employee where deleted = true);

delete from employee
    where deleted = true;

alter table employee
    drop column deleted;
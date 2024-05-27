-- ------------------------------
-- Employee
-- ------------------------------

-- add deleted column to employee
alter table employee
    add column deleted bit default 0;

alter table employee modify deleted bit not null default 0;

-- remove unique constraint from employee's user column
alter table employee drop foreign key FK_Employee_User;
drop index UK_Employee_User on employee;
alter table employee add constraint FK_Employee_User foreign key (user_id) references user_entity (id);

-- add non unique index on user_id again
create index IX_Emplyee_User on employee (user_id);

-- ------------------------------
-- Company
-- ------------------------------

-- add deleted column to company
alter table company
    add column deleted bit default 0;

alter table company modify deleted bit not null default 0;

-- make name column nullable
alter table company modify name varchar(100);

-- ------------------------------
-- Company kafka events
-- ------------------------------
drop table company_kafka_event;
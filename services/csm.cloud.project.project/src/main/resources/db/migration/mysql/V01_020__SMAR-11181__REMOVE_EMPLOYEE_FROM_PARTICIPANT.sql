alter table project_participant
    drop index UK_ProjPart_ProjEmpl;

alter table project_participant
    drop foreign key FK_ProjPart_Employee;

alter table project_participant
    drop column employee_id;

alter table workday_configuration_working_days
    modify working_days varchar (12) not null;

alter table workday_configuration_working_days
    add primary key (workday_configuration_project_id, working_days);
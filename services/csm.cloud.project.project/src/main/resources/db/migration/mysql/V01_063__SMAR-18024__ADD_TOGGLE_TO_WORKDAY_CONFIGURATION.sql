alter table workday_configuration
    add column allow_work_on_non_working_days bit;

update workday_configuration
set allow_work_on_non_working_days = 1;

alter table workday_configuration
    modify allow_work_on_non_working_days bit not null;


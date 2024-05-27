create table workday_configuration_working_days
(
    workday_configuration_project_id bigint       not null,
    working_days                     varchar(255) not null
) engine=InnoDB;

alter table workday_configuration_working_days
    add constraint FK_WorkdayConfiguration_WorkingDays
    foreign key (workday_configuration_project_id)
    references workday_configuration (project_id);

create index IX_WorkConfWorkDays_WorkConfProj on workday_configuration_working_days (workday_configuration_project_id);

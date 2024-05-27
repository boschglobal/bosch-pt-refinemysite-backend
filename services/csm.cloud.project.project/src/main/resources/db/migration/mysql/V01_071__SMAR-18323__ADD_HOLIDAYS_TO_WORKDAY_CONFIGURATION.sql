create table workday_configuration_holidays
(
    workday_configuration_project_id bigint       not null,
    date                             date         not null,
    name                             varchar(100) not null,
    primary key (workday_configuration_project_id, date, name)
) engine=InnoDB;

alter table workday_configuration_holidays
    add constraint FK_WorkdayConfiguration_Holidays
    foreign key (workday_configuration_project_id)
    references workday_configuration (project_id);

create index IX_WorkConfHoli_WorkConfProj on workday_configuration_holidays (workday_configuration_project_id);

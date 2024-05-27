create table workday_configuration
(
    created_by         varchar(36) not null,
    created_date       datetime(6) not null,
    identifier         varchar(36) not null,
    last_modified_by   varchar(36) not null,
    last_modified_date datetime(6) not null,
    version            bigint      not null,
    start_of_week      varchar(12) not null,
    project_id         bigint      not null,
    primary key (project_id)
) engine=InnoDB;

alter table workday_configuration
    add constraint UK_WorkdayConfiguration_Identifier unique (identifier);

alter table workday_configuration
    add constraint FK_WorkdayConfiguration_Project
    foreign key (project_id)
    references project (id);

create index IX_WorkConf_Projct on workday_configuration (project_id);

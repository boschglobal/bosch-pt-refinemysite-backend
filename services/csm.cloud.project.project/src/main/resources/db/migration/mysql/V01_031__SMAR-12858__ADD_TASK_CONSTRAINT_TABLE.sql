create table task_constraint_customization
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6)  not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6)  not null,
    version             bigint       not null,
    active              bit          not null,
    tsk_con_key         integer      not null,
    name                varchar(50),
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    project_id          bigint       not null,
    primary key (id)
) engine = InnoDB;

alter table task_constraint_customization
    add constraint UK_TskConCust_Identifier unique (identifier);

alter table task_constraint_customization
    add constraint FK_TskConCust_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table task_constraint_customization
    add constraint FK_TskConCust_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

alter table task_constraint_customization
    add constraint FK_TskConCust_Project
        foreign key (project_id)
            references project (id);

create index IX_TaskConsCust_CreaBy on task_constraint_customization (created_by_id);

create index IX_TaskConsCust_LastModiBy on task_constraint_customization (last_modified_by_id);

create index IX_TaskConsCust_Projct on task_constraint_customization (project_id);

create table milestone
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6)  not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6)  not null,
    version             bigint       not null,
    date                date         not null,
    description         varchar(1000),
    header              bit          not null,
    name                varchar(100) not null,
    type                integer      not null,
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    craft_id            bigint,
    project_id          bigint       not null,
    work_area_id        bigint,
    primary key (id)
) engine = InnoDB;#

alter table milestone
    add constraint UK_Milestone_Identifier unique (identifier);

alter table milestone
    add constraint FK_Milestone_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table milestone
    add constraint FK_Milestone_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

alter table milestone
    add constraint FK_Milestone_Craft
        foreign key (craft_id)
            references project_craft (id);

alter table milestone
    add constraint FK_Milestone_Project
        foreign key (project_id)
            references project (id);

alter table milestone
    add constraint FK_Milestone_Workarea
        foreign key (work_area_id)
            references work_area (id);
create table project_craft_list
(
    id                 bigint      not null auto_increment,
    created_by         varchar(36) not null,
    created_date       datetime(6) not null,
    identifier         varchar(36) not null,
    last_modified_by   varchar(36) not null,
    last_modified_date datetime(6) not null,
    version            bigint      not null,
    project_id         bigint      not null,
    primary key (id)
) engine = InnoDB;

alter table project_craft
    add column position              integer,
    add column project_craft_list_id bigint,
    add constraint FK_ProjectCraft_ProjectCraftList
        foreign key (project_craft_list_id)
            references project_craft_list (id);

alter table project_craft_list
    add constraint UK_ProjectCraftList_Identifier unique (identifier),
    add constraint UK_ProjectCraftList_Project unique (project_id),
    add constraint FK_ProjectCraftList_Project
        foreign key (project_id)
            references project (id);

create index IX_ProjCraf_ProjCrafList on project_craft (project_craft_list_id);
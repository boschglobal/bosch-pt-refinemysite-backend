create table relation
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6)  not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6)  not null,
    version             bigint       not null,
    source_identifier   varchar(36)  not null,
    source_type         varchar(30)  not null,
    target_identifier   varchar(36)  not null,
    target_type         varchar(30)  not null,
    type                varchar(30)  not null,
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    project_id          bigint       not null,
    primary key (id)
) engine = InnoDB;


alter table relation
    add constraint UK_Relation_Identifier unique (identifier);

alter table relation
    add constraint UK_Relation unique (type, source_identifier, source_type, target_identifier, target_type);

alter table relation
    add constraint FK_Relation_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table relation
    add constraint FK_Relation_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

alter table relation
    add constraint FK_Relation_Project
        foreign key (project_id)
            references project (id);

create index IX_Relaion_CreaBy on relation (created_by_id);

create index IX_Relaion_LastModiBy on relation (last_modified_by_id);

create index IX_Relaion_Projct on relation (project_id);
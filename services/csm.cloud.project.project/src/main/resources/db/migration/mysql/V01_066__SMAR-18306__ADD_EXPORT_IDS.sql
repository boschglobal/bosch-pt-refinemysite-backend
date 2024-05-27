create table external_id
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6) not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6) not null,
    version             bigint       not null,
    file_id             integer,
    file_unique_id      integer,
    guid                varchar(255),
    id_type             integer      not null,
    object_identifier   varchar(255) not null,
    object_type         varchar(255) not null,
    project_id          varchar(255) not null,
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    primary key (id)
) engine=InnoDB;

create index IX_ExternalId_ProjType on external_id (project_id, id_type);

alter table external_id
    add constraint FK_ExternalId_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table external_id
    add constraint FK_ExternalId_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

create index IX_Extenal_CreaBy on external_id (created_by_id);

create index IX_Extenal_LastModiBy on external_id (last_modified_by_id);

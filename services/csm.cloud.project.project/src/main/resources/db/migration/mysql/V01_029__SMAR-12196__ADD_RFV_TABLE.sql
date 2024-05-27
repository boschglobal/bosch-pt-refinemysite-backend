create table rfv_customization
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6) not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6) not null,
    version             bigint       not null,
    active              bit          not null,
    rfv_key             integer      not null,
    name                varchar(50),
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    project_id          bigint       not null,
    primary key (id)
) engine = InnoDB;

alter table rfv_customization
    add constraint UK_RfvCust_Identifier unique (identifier);

alter table rfv_customization
    add constraint FK_RfvCust_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table rfv_customization
    add constraint FK_RfvCust_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

alter table rfv_customization
    add constraint FK_RfvCust_Project
        foreign key (project_id)
            references project (id);

create
index IX_RfvCust_CreaBy on rfv_customization (created_by_id);

create
index IX_RfvCust_LastModiBy on rfv_customization (last_modified_by_id);

create
index IX_RfvCust_Projct on rfv_customization (project_id);
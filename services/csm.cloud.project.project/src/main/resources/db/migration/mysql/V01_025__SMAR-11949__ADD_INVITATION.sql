create table invitation
(
    id                     bigint       not null auto_increment,
    created_date           datetime(6) not null,
    identifier             varchar(255) not null,
    last_modified_date     datetime(6) not null,
    version                bigint       not null,
    email                  varchar(255) not null,
    last_sent              datetime(6) not null,
    participant_identifier varchar(255) not null,
    project_identifier     varchar(255) not null,
    created_by_id          bigint       not null,
    last_modified_by_id    bigint       not null,
    primary key (id)
) engine=InnoDB;

create table invitation_kafka_event
(
    id                 bigint not null auto_increment,
    event              longblob,
    event_key          longblob,
    partition_number   integer,
    trace_header_key   varchar(255),
    trace_header_value varchar(255),
    primary key (id)
) engine=InnoDB;

alter table invitation
    add constraint UK_Invitation_Identifier unique (identifier);

alter table invitation
    add constraint UK_Invitation_Part_Identifier unique (participant_identifier);

alter table invitation
    add constraint UK_Invitation_Email unique (project_identifier, email);

alter table invitation
    add constraint FK_Invitation_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table invitation
    add constraint FK_Invitation_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);
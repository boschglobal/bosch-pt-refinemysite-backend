/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

create table pat_kafka_event
(
    id                     bigint       not null auto_increment,
    event                  mediumblob,
    event_key              mediumblob   not null,
    partition_number       integer      not null,
    trace_header_key       varchar(255) not null,
    trace_header_value     varchar(255) not null,
    transaction_identifier varchar(36),
    primary key (id)
) engine = InnoDB;

create table pat_entity
(
    id                 bigint       not null auto_increment,
    created_by         varchar(36)  not null,
    created_date       datetime(6)  not null,
    identifier         varchar(36)  not null,
    last_modified_by   varchar(36)  not null,
    last_modified_date datetime(6)  not null,
    version            bigint       not null,
    description        varchar(128) not null,
    expires_at         datetime(6)  not null,
    hash               varchar(512) not null,
    impersonated_user  varchar(36)  not null,
    issued_at          datetime(6)  not null,
    type               varchar(8)   not null,
    primary key (id)
) engine = InnoDB;

create table pat_scope
(
    pat_id bigint not null,
    scopes enum ('GRAPHQL_API_READ','TIMELINE_API_READ')
) engine = InnoDB;

alter table pat_entity
    add constraint UK_Hash unique (hash);

alter table pat_scope
    add constraint FK_PAT_Scope_PatId
        foreign key (pat_id)
            references pat_entity (id);

create index IX_PatScop_Pat on pat_scope (pat_id);
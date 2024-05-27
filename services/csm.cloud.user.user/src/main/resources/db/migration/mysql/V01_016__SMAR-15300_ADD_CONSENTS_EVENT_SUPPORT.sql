-- add consents kafka event table
create table consents_kafka_event
(
    id                     bigint       not null auto_increment,
    event                  longblob,
    event_key              longblob     not null,
    partition_number       integer      not null,
    trace_header_key       varchar(255) not null,
    trace_header_value     varchar(255) not null,
    transaction_identifier varchar(36),
    primary key (id)
) engine=InnoDB;

-- add snapshot entity columns
alter table user_consent_delay
    add column (created_by varchar(255) not null,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_by varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null);

-- refactor consents
drop index IX_UserConsentDelay_UserIdentifier on user_consent_delay;

alter table user_consent_delay rename consents_user;

alter table consents_user drop column user_identifier;

alter table consents_user
    add constraint IX_ConsentsUser_Identifier unique (identifier);

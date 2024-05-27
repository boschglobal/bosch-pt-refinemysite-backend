create table event_of_business_transaction
(
    id                     bigint       not null auto_increment,
    creation_date          datetime(6)  not null,
    event_key              blob         not null,
    event_key_class        varchar(255) not null,
    event_processor_name   varchar(50)  not null,
    event_value            blob         not null,
    event_value_class      varchar(255) not null,
    message_date           datetime(6)  not null,
    transaction_identifier varchar(36)  not null,
    primary key (id)
) engine = InnoDB;


create index IX_EventOfBusinessTransaction_TidEventProcessor on event_of_business_transaction (transaction_identifier, event_processor_name);
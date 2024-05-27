alter table craft_kafka_event
    add column transaction_identifier varchar(36);

alter table user_kafka_event
    add column transaction_identifier varchar(36);
alter table event_of_business_transaction
    add column consumer_offset bigint not null default 0;
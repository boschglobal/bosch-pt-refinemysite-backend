alter table user_entity
    add column locked bit not null default 0;
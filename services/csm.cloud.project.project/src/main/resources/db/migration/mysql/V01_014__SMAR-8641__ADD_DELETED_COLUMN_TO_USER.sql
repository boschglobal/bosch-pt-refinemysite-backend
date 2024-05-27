alter table user_entity
  add column deleted bit default 0;

alter table user_entity modify deleted bit not null default 0;
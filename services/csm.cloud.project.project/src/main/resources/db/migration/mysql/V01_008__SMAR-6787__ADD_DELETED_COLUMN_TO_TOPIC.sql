alter table topic
  add column deleted bit default 0;

alter table topic modify deleted bit not null default 0;
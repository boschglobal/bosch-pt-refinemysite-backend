alter table project
  add column deleted bit default 0;

alter table project modify deleted bit not null default 0;
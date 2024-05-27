alter table task
  add column deleted bit default 0;

alter table task modify deleted bit not null default 0;
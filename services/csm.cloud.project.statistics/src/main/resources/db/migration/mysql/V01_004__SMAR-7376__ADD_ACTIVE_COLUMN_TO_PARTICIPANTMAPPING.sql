alter table participant_mapping
  add column active bit;

update participant_mapping set active = 1;

alter table participant_mapping modify active bit not null;
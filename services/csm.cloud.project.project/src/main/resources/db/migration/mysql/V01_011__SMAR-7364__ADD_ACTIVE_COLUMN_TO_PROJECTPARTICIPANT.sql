alter table project_participant
  add column active bit;

update project_participant set active = 1;

alter table project_participant modify active bit not null;
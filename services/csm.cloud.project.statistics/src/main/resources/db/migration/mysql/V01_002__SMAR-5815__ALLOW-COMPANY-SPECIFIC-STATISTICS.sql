alter table day_card_statistics
  add column assigned_participant_id bigint,
  add column craft_identifier varchar(255),
  add column task_identifier varchar(255);

create table named_object (
  id bigint not null auto_increment,
  name varchar(255),
  identifier varchar(255),
  type varchar(255),
  primary key (id)
) engine=InnoDB;

alter table participant_mapping
  add column company_identifier varchar(255),
  add column participant_identifier varchar(255);
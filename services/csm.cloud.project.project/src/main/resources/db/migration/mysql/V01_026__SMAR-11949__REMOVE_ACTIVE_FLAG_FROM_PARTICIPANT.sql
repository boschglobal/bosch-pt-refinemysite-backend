drop trigger set_participant_active_on_insert;
drop trigger set_participant_active_on_update;
drop trigger set_participant_status_on_insert;
drop trigger set_participant_status_on_update;

alter table project_participant
    drop column active;
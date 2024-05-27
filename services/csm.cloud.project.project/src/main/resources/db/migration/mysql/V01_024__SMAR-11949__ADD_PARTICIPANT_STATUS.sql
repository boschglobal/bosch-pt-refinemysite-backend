alter table project_participant
    add column status integer;

delimiter $$
create trigger set_participant_status_on_insert before insert on project_participant for each row
begin
    if new.active is not null then
        set new.status = case when new.active = false then 400 else 300 end;
    end if;
end$$
delimiter ;

delimiter $$
create trigger set_participant_status_on_update before update on project_participant for each row
begin
    if new.active <> old.active then
        set new.status = case when new.active = false then 400 else 300 end;
    end if;
end$$
delimiter ;

delimiter $$
create trigger set_participant_active_on_insert before insert on project_participant for each row
begin
    if new.status is not null then
        set new.active = case when new.status = 400 then false else true end;
    end if;
end$$
delimiter ;

delimiter $$
create trigger set_participant_active_on_update before update on project_participant for each row
begin
    if new.status <> old.status then
        set new.active = case when new.status = 400 then false else true end;
end if;
end$$
delimiter ;

update project_participant
set status = (case when active = false then 400 else 300 end);

alter table project_participant
    modify column status integer not null;
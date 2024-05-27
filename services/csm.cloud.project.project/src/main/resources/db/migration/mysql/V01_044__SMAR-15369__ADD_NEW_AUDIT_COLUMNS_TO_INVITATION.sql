alter table invitation
    add column created_by VARCHAR(255) not null;
alter table invitation
    add column last_modified_by VARCHAR(255) not null;

delimiter $$
create trigger set_invitation_audit_columns_on_insert before insert on invitation for each row
begin
    if new.created_by is null then
        set new.created_by = (SELECT identifier from user_entity u where u.id = new.created_by_id);
        set new.last_modified_by = (SELECT identifier from user_entity u where u.id = new.last_modified_by_id);
    elseif new.created_by_id is null then
        set new.created_by_id = (SELECT id from user_entity u where u.identifier = new.created_by);
        set new.last_modified_by_id = (SELECT id from user_entity u where u.identifier = new.last_modified_by);
    end if;
end$$
delimiter ;

delimiter $$
create trigger set_invitation_audit_columns_on_update before update on invitation for each row
begin
    if new.last_modified_by_id <> old.last_modified_by_id then
        set new.last_modified_by = (SELECT identifier from user_entity u where u.id = new.last_modified_by_id);
    elseif new.last_modified_by <> old.last_modified_by then
        set new.last_modified_by_id = (SELECT id from user_entity u where u.identifier = new.last_modified_by);
    end if;
end$$
delimiter ;

update invitation i
    left join user_entity u
    on i.created_by_id = u.id
    set i.created_by = u.identifier;

update invitation i
    left join user_entity u
    on i.last_modified_by_id = u.id
    set i.last_modified_by = u.identifier;

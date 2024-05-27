alter table work_area_list
    add column created_by VARCHAR(36) not null;
alter table work_area_list
    add column last_modified_by VARCHAR(36) not null;

delimiter $$
create trigger set_work_area_list_audit_columns_on_insert before insert on work_area_list for each row
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
create trigger set_work_area_list_audit_columns_on_update before update on work_area_list for each row
begin
    if new.last_modified_by_id <> old.last_modified_by_id then
        set new.last_modified_by = (SELECT identifier from user_entity u where u.id = new.last_modified_by_id);
    elseif new.last_modified_by <> old.last_modified_by then
        set new.last_modified_by_id = (SELECT id from user_entity u where u.identifier = new.last_modified_by);
end if;
end$$
delimiter ;

update work_area_list wal
    left join user_entity u
on wal.created_by_id = u.id
    set wal.created_by = u.identifier;

update work_area_list wal
    left join user_entity u
on wal.last_modified_by_id = u.id
    set wal.last_modified_by = u.identifier;

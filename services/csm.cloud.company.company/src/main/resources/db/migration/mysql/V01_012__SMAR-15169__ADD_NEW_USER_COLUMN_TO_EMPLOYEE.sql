alter table employee
    add column user_ref VARCHAR(255) not null;

delimiter $$
create trigger set_employee_user_ref_on_insert before insert on employee for each row
begin
    if new.user_ref is null then
        set new.user_ref = (SELECT identifier from user_entity u where u.id = new.user_id);
    elseif new.user_id is null then
        set new.user_id = (SELECT id from user_entity u where u.identifier = new.user_ref);
    end if;
end$$
delimiter ;

update employee e
    left join user_entity u
    on e.user_id = u.id
    set e.user_ref = u.identifier;

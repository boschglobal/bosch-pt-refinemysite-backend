alter table profile_picture
    add column created_by VARCHAR(255);
alter table profile_picture
    add column last_modified_by VARCHAR(255);

alter table user_entity
    add column created_by VARCHAR(255);
alter table user_entity
    add column last_modified_by VARCHAR(255);

alter table craft
    add column created_by VARCHAR(255);
alter table craft
    add column last_modified_by VARCHAR(255);

update profile_picture pp
    left join user_entity u
on pp.created_by_id = u.id
    set pp.created_by = u.identifier;

update profile_picture pp
    left join user_entity u
on pp.last_modified_by_id = u.id
    set pp.last_modified_by = u.identifier;

update user_entity ue
    left join user_entity u
on ue.created_by_id = u.id
    set ue.created_by = u.identifier;

update user_entity ue
    left join user_entity u
on ue.last_modified_by_id = u.id
    set ue.last_modified_by = u.identifier;

update craft c
    left join user_entity u
on c.created_by_id = u.id
    set c.created_by = u.identifier;

update craft c
    left join user_entity u
on c.last_modified_by_id = u.id
    set c.last_modified_by = u.identifier;

alter table profile_picture drop foreign key FK_ProfilePicture_CreatedBy;
alter table profile_picture drop foreign key FK_ProfilePicture_LastModifiedBy;

alter table user_entity drop foreign key FK_User_CreatedBy;
alter table user_entity drop foreign key FK_User_LastModifiedBy;

alter table craft drop foreign key FK_Craft_CreatedBy;
alter table craft drop foreign key FK_Craft_LastModifiedBy;

alter table profile_picture drop column created_by_id;
alter table profile_picture drop column last_modified_by_id;

alter table user_entity drop column created_by_id;
alter table user_entity drop column last_modified_by_id;

alter table craft drop column created_by_id;
alter table craft drop column last_modified_by_id;
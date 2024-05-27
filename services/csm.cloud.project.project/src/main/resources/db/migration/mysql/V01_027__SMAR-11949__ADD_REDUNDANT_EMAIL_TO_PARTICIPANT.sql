alter table project_participant
    add column email varchar(255);

update project_participant pp
    left join user_entity u
on pp.user_id = u.id
    set pp.email = u.email;

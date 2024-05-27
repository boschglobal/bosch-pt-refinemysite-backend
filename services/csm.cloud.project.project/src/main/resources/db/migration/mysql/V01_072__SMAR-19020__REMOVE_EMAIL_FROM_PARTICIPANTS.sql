update project_participant pp
    inner join user_entity u
on pp.user_id = u.id
    set pp.email = u.email
where pp.email != u.email
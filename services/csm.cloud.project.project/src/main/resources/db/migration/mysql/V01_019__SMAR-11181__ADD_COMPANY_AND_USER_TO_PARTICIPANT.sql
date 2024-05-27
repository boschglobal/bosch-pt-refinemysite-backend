alter table project_participant
    add column company_id bigint;

alter table project_participant
    add column user_id bigint;

alter table project_participant
    add constraint FK_ProjPart_Company
        foreign key (company_id)
            references company (id);

alter table project_participant
    add constraint FK_ProjPart_User
        foreign key (user_id)
            references user_entity (id);

create index IX_ProjPart_Compny on project_participant (company_id);

create index IX_ProjPart_User on project_participant (user_id);

alter table project_participant
    add constraint UK_ProjPart_Assignment unique (project_id, company_id, user_id);

update project_participant pp
    left join employee e on pp.employee_id = e.id
    left join user_entity u on e.user_id = u.id
set pp.user_id = u.id;

update project_participant pp
    left join employee e on pp.employee_id = e.id
    left join company c on e.company_id = c.id
set pp.company_id = c.id;
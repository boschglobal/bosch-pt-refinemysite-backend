SET FOREIGN_KEY_CHECKS = 0;
alter table projection_employable_user_company_name
    modify id bigint not null;
SET FOREIGN_KEY_CHECKS = 1;

alter table projection_employable_user_company_name
drop primary key,
    add primary key (identifier);

alter table projection_employable_user_company_name
drop column id;

alter table projection_employable_user_company_name
drop index UK_Identifier;
create table projection_employable_user
(
    id                  bigint       not null auto_increment,
    admin               bit          not null,
    company_identifier  varchar(255),
    company_name        varchar(255),
    email               varchar(255),
    employee_identifier varchar(255),
    first_name          varchar(255),
    gender              varchar(255),
    identifier          varchar(255) not null,
    last_name           varchar(255),
    locked              bit          not null,
    user_created_date   datetime(6),
    user_country        varchar(255),
    user_name           varchar(255),
    primary key (id)
) engine=InnoDB;

create index IX_UserName_CompanyName on projection_employable_user (user_name, company_name);
create index IX_CompanyName_UserName on projection_employable_user (company_name, user_name);

alter table projection_employable_user
    add constraint UK_Identifier unique (identifier);

alter table projection_employable_user
    add constraint UK_Email unique (email);

create table projection_employable_user_company_name
(
    id           bigint       not null auto_increment,
    company_name varchar(255) not null,
    identifier   varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table projection_employable_user_company_name
    add constraint UK_Identifier unique (identifier);
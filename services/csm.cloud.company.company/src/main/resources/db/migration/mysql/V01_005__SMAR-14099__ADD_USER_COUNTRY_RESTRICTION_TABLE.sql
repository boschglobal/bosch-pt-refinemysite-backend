create table user_country_restriction
(
    id           bigint       not null auto_increment,
    country_code varchar(255) not null,
    user_id      varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table user_country_restriction
    add constraint UK_UserCountryRestriction_User_Country unique (user_id, country_code);

create index IX_UserCountryRestriction_User on user_country_restriction (user_id);
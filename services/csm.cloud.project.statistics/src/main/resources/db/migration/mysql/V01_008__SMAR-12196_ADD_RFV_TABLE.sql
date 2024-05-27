create table rfv_customization
(
    id                 bigint       not null auto_increment,
    active             bit          not null,
    identifier         varchar(255) not null,
    rfv_key            integer      not null,
    name               varchar(255),
    project_identifier varchar(255) not null,
    primary key (id)
) engine = InnoDB;

alter table rfv_customization
    add constraint UK_RfvCust_Identifier unique (identifier);

alter table rfv_customization
    add constraint UK_RfvCust_ProjId unique (project_identifier, identifier);
create table projection_user
(
    identifier           varchar(255) not null,
    admin                bit          not null,
    ciam_user_identifier varchar(255) not null,
    created_by           varchar(255) not null,
    created_date         datetime(6),
    email                varchar(255) not null,
    first_name           varchar(255) not null,
    last_modified_by     varchar(255) not null,
    last_modified_date   datetime(6),
    last_name            varchar(255) not null,
    locale               varchar(255),
    locked               bit          not null,
    version              bigint       not null,
    primary key (identifier)
) engine = InnoDB;

alter table projection_user
    add constraint UK_Email unique (email);

alter table projection_user
    add constraint UK_CiamUserIdentifier unique (ciam_user_identifier);
create table announcement (
    id bigint not null auto_increment,
    identifier varchar(255) not null,
    type integer not null,
    primary key (id)
) engine=InnoDB;

create table announcement_permission (
    id bigint not null auto_increment,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table announcement_translation (
    announcement_id bigint not null,
    locale varchar(255) not null,
    value varchar(255) not null
) engine=InnoDB;

alter table announcement
    add constraint UK_Announcement_Identifier unique (identifier);

alter table announcement_permission
    add constraint UK_Announcement_Permission_User unique (user_id);

alter table announcement_translation
    add constraint UK_Announcement_Translation_Lang unique (announcement_id, locale);

alter table announcement_permission
    add constraint FK_AnnouncementPermission_User
    foreign key (user_id)
    references user_entity (id);

alter table announcement_translation
    add constraint FK_Announcement_Translation_AnnouncementId
    foreign key (announcement_id)
    references announcement (id);

create index IX_AnnoTran_Annoment on announcement_translation (announcement_id);
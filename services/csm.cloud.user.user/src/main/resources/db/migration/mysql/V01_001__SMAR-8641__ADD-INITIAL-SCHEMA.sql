
    create table craft (
       id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        default_name varchar(128) not null,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table craft_translation (
       craft_id bigint not null,
        locale varchar(255) not null,
        value varchar(255) not null
    ) engine=InnoDB;

    create table craft_kafka_event (
       id bigint not null auto_increment,
        event longblob,
        event_key longblob,
        partition_number integer,
        trace_header_key varchar(255),
        trace_header_value varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table profile_picture (
       id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        file_size bigint not null,
        full_available bit,
        height bigint not null,
        small_available bit,
        width bigint not null,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_craft (
       user_id bigint not null,
        craft_id bigint not null,
        primary key (user_id, craft_id)
    ) engine=InnoDB;

    create table user_entity (
       id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        admin bit not null,
        email varchar(255) not null,
        first_name varchar(50),
        gender varchar(20),
        last_name varchar(50),
        position varchar(100),
        registered bit not null,
        user_id varchar(100) not null,
        created_by_id bigint,
        last_modified_by_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table user_phonenumber (
       user_id bigint not null,
        call_number varchar(25) not null,
        country_code varchar(5) not null,
        phone_number_type varchar(255) not null,
        primary key (user_id, call_number, country_code, phone_number_type)
    ) engine=InnoDB;

    create table user_kafka_event (
       id bigint not null auto_increment,
        event longblob,
        event_key longblob,
        partition_number integer,
        trace_header_key varchar(255),
        trace_header_value varchar(255),
        primary key (id)
    ) engine=InnoDB;

    alter table craft
       add constraint UK_Craft_Identifier unique (identifier);

    alter table craft_translation
       add constraint UK_CRAFT_TRANSLATION_LANG unique (craft_id, locale);

    alter table profile_picture
       add constraint UK_ProfilePicture_UserId unique (user_id);

    alter table user_entity
       add constraint UK_UserId unique (user_id);

    alter table user_entity
       add constraint UK_User_Identifier unique (identifier);

    alter table user_entity
       add constraint UK_User_Email unique (email);

    alter table craft
       add constraint FK_Craft_CreatedBy
       foreign key (created_by_id)
       references user_entity (id);

    alter table craft
       add constraint FK_Craft_LastModifiedBy
       foreign key (last_modified_by_id)
       references user_entity (id);

    alter table craft_translation
       add constraint FK_Craft_Translation_CraftId
       foreign key (craft_id)
       references craft (id);

    alter table profile_picture
       add constraint FK_ProfilePicture_CreatedBy
       foreign key (created_by_id)
       references user_entity (id);

    alter table profile_picture
       add constraint FK_ProfilePicture_LastModifiedBy
       foreign key (last_modified_by_id)
       references user_entity (id);

    alter table profile_picture
       add constraint FK_ProfilePicture_User
       foreign key (user_id)
       references user_entity (id);

    alter table user_craft
       add constraint FK_User_Craft_CraftId
       foreign key (craft_id)
       references craft (id);

    alter table user_craft
       add constraint FK_User_Craft_UserId
       foreign key (user_id)
       references user_entity (id);

    alter table user_entity
       add constraint FK_User_CreatedBy
       foreign key (created_by_id)
       references user_entity (id);

    alter table user_entity
       add constraint FK_User_LastModifiedBy
       foreign key (last_modified_by_id)
       references user_entity (id);

    alter table user_phonenumber
       add constraint FK_User_PhoneNumber_UserId
       foreign key (user_id)
       references user_entity (id);

create index IX_Craf_CreaBy on craft (created_by_id);

create index IX_Craf_LastModiBy on craft (last_modified_by_id);

create index IX_CrafTran_Craf on craft_translation (craft_id);

create index IX_ProfPict_CreaBy on profile_picture (created_by_id);

create index IX_ProfPict_LastModiBy on profile_picture (last_modified_by_id);

create index IX_UserCraf_Craf on user_craft (craft_id);

create index IX_UserCraf_User on user_craft (user_id);

create index IX_UserEnti_CreaBy on user_entity (created_by_id);

create index IX_UserEnti_LastModiBy on user_entity (last_modified_by_id);

create index IX_UserPhon_User on user_phonenumber (user_id);



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

    create table consents_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table consents_user (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        delayed_at datetime(6) not null,
        primary key (id)
    ) engine=InnoDB;

    create table craft (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        default_name varchar(128) not null,
        primary key (id)
    ) engine=InnoDB;

    create table craft_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table craft_translation (
        craft_id bigint not null,
        locale varchar(255) not null,
        value varchar(255) not null
    ) engine=InnoDB;

    create table document (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        client varchar(32) not null,
        country varchar(32) not null,
        document_type varchar(32) not null,
        locale varchar(32) not null,
        title varchar(255) not null,
        url varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table document_version (
        document_id bigint not null,
        identifier varchar(255) not null,
        last_changed datetime(6) not null
    ) engine=InnoDB;

    create table pat_entity (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        description varchar(128) not null,
        expires_at datetime(6) not null,
        hash varchar(512) not null,
        impersonated_user varchar(36) not null,
        issued_at datetime(6) not null,
        type varchar(8) not null,
        primary key (id)
    ) engine=InnoDB;

    create table pat_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table pat_scope (
        pat_id bigint not null,
        scopes varchar(32)
    ) engine=InnoDB;

    create table profile_picture (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        file_size bigint not null,
        full_available bit,
        height bigint not null,
        small_available bit,
        width bigint not null,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_consent (
        consents_user_id bigint not null,
        date date not null,
        document_version_id varchar(255) not null
    ) engine=InnoDB;

    create table user_country_restriction (
        id bigint not null auto_increment,
        country_code varchar(255) not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_craft (
        user_id bigint not null,
        craft_id bigint not null,
        primary key (user_id, craft_id)
    ) engine=InnoDB;

    create table user_entity (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        admin bit not null,
        country varchar(255),
        email varchar(255) not null,
        eula_accepted_date date,
        user_id varchar(100) not null,
        first_name varchar(50),
        gender varchar(20),
        last_name varchar(50),
        locale varchar(255),
        locked bit not null,
        position varchar(100),
        registered bit not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table user_phonenumber (
        user_id bigint not null,
        call_number varchar(25) not null,
        country_code varchar(5) not null,
        phone_number_type varchar(255) not null,
        primary key (user_id, call_number, country_code, phone_number_type)
    ) engine=InnoDB;

    create table user_deletion_state (
        id bigint not null auto_increment,
        deleted_to_date_time datetime(6),
        primary key (id)
    ) engine=InnoDB;

    alter table announcement 
       add constraint UK_Announcement_Identifier unique (identifier);

    alter table announcement_permission 
       add constraint UK_Announcement_Permission_User unique (user_id);

    alter table announcement_translation 
       add constraint UK_Announcement_Translation_Lang unique (announcement_id, locale);

    alter table consents_user 
       add constraint IX_ConsentsUser_Identifier unique (identifier);

    alter table craft 
       add constraint UK_Craft_Identifier unique (identifier);

    alter table craft_translation 
       add constraint UK_CRAFT_TRANSLATION_LANG unique (craft_id, locale);

    alter table document 
       add constraint UK_Document_Type_Country_Locale_Client unique (document_type, country, locale, client);

    alter table document_version 
       add constraint UK_DocumentVersion_Document_LastChanged unique (document_id, last_changed);

    alter table document_version 
       add constraint UK_DocumentVersion_Identifier unique (identifier);

    alter table pat_entity 
       add constraint UK_Hash unique (hash);

    alter table profile_picture 
       add constraint UK_ProfilePicture_UserId unique (user_id);

    create index IX_UserCountryRestriction_User 
       on user_country_restriction (user_id);

    alter table user_country_restriction 
       add constraint UK_UserCountryRestriction_User_Country unique (user_id, country_code);

    alter table user_entity 
       add constraint UK_UserId unique (user_id);

    alter table user_entity 
       add constraint UK_User_Identifier unique (identifier);

    alter table user_entity 
       add constraint UK_User_Email unique (email);

    alter table announcement_permission 
       add constraint FK_AnnouncementPermission_User 
       foreign key (user_id) 
       references user_entity (id);

    alter table announcement_translation 
       add constraint FK_Announcement_Translation_AnnouncementId 
       foreign key (announcement_id) 
       references announcement (id);

    alter table craft_translation 
       add constraint FK_Craft_Translation_CraftId 
       foreign key (craft_id) 
       references craft (id);

    alter table document_version 
       add constraint FK_DocumentVersion_Document 
       foreign key (document_id) 
       references document (id);

    alter table pat_scope 
       add constraint FK_PAT_Scope_PatId 
       foreign key (pat_id) 
       references pat_entity (id);

    alter table profile_picture 
       add constraint FK_ProfilePicture_User 
       foreign key (user_id) 
       references user_entity (id);

    alter table user_consent 
       add constraint FK_UserConsent_ConsentsUser 
       foreign key (consents_user_id) 
       references consents_user (id);

    alter table user_craft 
       add constraint FK_User_Craft_CraftId 
       foreign key (craft_id) 
       references craft (id);

    alter table user_craft 
       add constraint FK_User_Craft_UserId 
       foreign key (user_id) 
       references user_entity (id);

    alter table user_phonenumber 
       add constraint FK_User_PhoneNumber_UserId 
       foreign key (user_id) 
       references user_entity (id);

create index IX_AnnoTran_Annoment on announcement_translation (announcement_id);

create index IX_CrafTran_Craf on craft_translation (craft_id);

create index IX_DocuVers_Docuent on document_version (document_id);

create index IX_PatScop_Pat on pat_scope (pat_id);

create index IX_UserCons_ConsUser on user_consent (consents_user_id);

create index IX_UserCraf_Craf on user_craft (craft_id);

create index IX_UserCraf_User on user_craft (user_id);

create index IX_UserPhon_User on user_phonenumber (user_id);


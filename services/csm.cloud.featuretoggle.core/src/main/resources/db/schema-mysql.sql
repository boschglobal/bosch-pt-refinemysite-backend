
    create table feature (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        name varchar(50) not null,
        state varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table feature_whitelisted_subject (
        feature_id bigint not null,
        feature_name varchar(50),
        subject_ref varchar(255) not null,
        type varchar(255) not null
    ) engine=InnoDB;

    create table featuretoggle_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table projection_user (
        identifier varchar(255) not null,
        admin bit not null,
        ciam_user_identifier varchar(255) not null,
        locale varchar(255),
        locked bit not null,
        version bigint not null,
        primary key (identifier)
    ) engine=InnoDB;

    alter table feature 
       add constraint UK_Feature_Name unique (name);

    alter table projection_user 
       add constraint UK_CiamUserIdentifier unique (ciam_user_identifier);

    alter table feature_whitelisted_subject 
       add constraint FK_Feature_WhitelistedSubject_FeatureId 
       foreign key (feature_id) 
       references feature (id);

create index IX_FeatWhitSubj_Featre on feature_whitelisted_subject (feature_id);


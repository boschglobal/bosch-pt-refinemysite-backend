
    create table day_card_statistics (
       id bigint not null auto_increment,
        context_identifier varchar(255),
        context_type varchar(255),
        project_identifier varchar(255),
        date date,
        reason integer,
        status integer,
        primary key (id)
    ) engine=InnoDB;

    create table object_relation (
       id bigint not null auto_increment,
        child_identifier varchar(255),
        child_type varchar(255),
        parent_identifier varchar(255),
        parent_type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table participant_mapping (
       id bigint not null auto_increment,
        participant_role varchar(255),
        project_identifier varchar(255),
        user_identifier varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table user_entity (
       id bigint not null auto_increment,
        identifier varchar(255),
        user_id varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;
create index IX_DayCardStatistics_ContIdentContTyp on day_card_statistics (context_identifier, context_type);
create index IX_DayCardStatistics_PrjIdDateStatus on day_card_statistics (project_identifier, date, status);

    alter table day_card_statistics
       add constraint UK_DayCardStatistics_PrjIdConIdentContTyp unique (project_identifier, context_identifier, context_type);

    alter table object_relation
       add constraint IX_ObjRel_ChildParent unique (child_identifier, child_type, parent_type);
create index IX_ParMap_ProjectRole on participant_mapping (project_identifier, participant_role);

    alter table participant_mapping
       add constraint IX_ParMap_ProjIdenUserIdent unique (project_identifier, user_identifier);

    alter table user_entity
       add constraint UK_User_UserId unique (user_id);

    alter table user_entity
       add constraint UK_User_Identifier unique (identifier);

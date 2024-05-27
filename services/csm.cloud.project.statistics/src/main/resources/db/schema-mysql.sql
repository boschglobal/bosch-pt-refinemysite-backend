
    create table day_card (
        id bigint not null auto_increment,
        context_identifier varchar(255),
        context_type varchar(255),
        project_identifier varchar(255),
        craft_identifier varchar(255),
        date date,
        reason tinyint check (reason between 0 and 14),
        status tinyint check (status between 0 and 3),
        task_identifier varchar(255),
        assigned_participant_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table named_object (
        id bigint not null auto_increment,
        name varchar(255),
        identifier varchar(255),
        type varchar(255),
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
        active bit not null,
        company_identifier varchar(255),
        participant_identifier varchar(255),
        participant_role varchar(255),
        project_identifier varchar(255),
        user_identifier varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table rfv_customization (
        id bigint not null auto_increment,
        active bit not null,
        identifier varchar(255) not null,
        rfv_key tinyint not null check (rfv_key between 0 and 14),
        name varchar(255),
        project_identifier varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_entity (
        id bigint not null auto_increment,
        admin bit not null,
        identifier varchar(255),
        locale varchar(255),
        locked bit not null,
        user_id varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;

    create index IX_DayCard_ContIdentContTyp 
       on day_card (context_identifier, context_type);

    create index IX_DayCard_TaskId 
       on day_card (task_identifier);

    alter table day_card 
       add constraint UK_DayCard_PrjIdConIdentContTyp unique (project_identifier, context_identifier, context_type);

    alter table named_object 
       add constraint UK_NamedObject_identifier unique (type, identifier);

    alter table object_relation 
       add constraint IX_ObjRel_ChildParent unique (child_identifier, child_type, parent_type);

    create index IX_ParMap_ProjectRole 
       on participant_mapping (project_identifier, participant_role);

    alter table participant_mapping 
       add constraint IX_ParMap_ProjIdCompIdUserId unique (project_identifier, user_identifier, company_identifier);

    alter table rfv_customization 
       add constraint UK_RfvCust_Identifier unique (identifier);

    alter table rfv_customization 
       add constraint UK_RfvCust_ProjId unique (project_identifier, identifier);

    alter table user_entity 
       add constraint UK_User_UserId unique (user_id);

    alter table user_entity 
       add constraint UK_User_Identifier unique (identifier);

    alter table day_card 
       add constraint FKtp5x2966jqpt52sbd20co7rpv 
       foreign key (assigned_participant_id) 
       references participant_mapping (id);

create index IX_DayCard_AssiPart on day_card (assigned_participant_id);


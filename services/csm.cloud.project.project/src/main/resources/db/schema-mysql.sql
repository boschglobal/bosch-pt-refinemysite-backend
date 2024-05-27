
    create table company (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        deleted bit not null,
        name varchar(100),
        post_box_address_post_box varchar(255),
        post_box_address_area varchar(255),
        post_box_address_city varchar(255),
        post_box_address_country varchar(255),
        post_box_address_zip_code varchar(255),
        street_address_house_number varchar(255),
        street_address_street varchar(255),
        street_address_area varchar(255),
        street_address_city varchar(255),
        street_address_country varchar(255),
        street_address_zip_code varchar(255),
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

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

    create table day_card (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        manpower decimal(38,2) not null,
        notes varchar(500),
        reason varchar(30),
        status varchar(10) not null,
        title varchar(100) not null,
        task_schedule_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table employee (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        company_id bigint not null,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table employee_role (
        employee_id bigint not null,
        roles varchar(255)
    ) engine=InnoDB;

    create table event_of_business_transaction (
        id bigint not null auto_increment,
        consumer_offset bigint not null,
        creation_date datetime(6) not null,
        event_key blob not null,
        event_key_class varchar(255) not null,
        event_processor_name varchar(50) not null,
        event_value blob not null,
        event_value_class varchar(255) not null,
        message_date datetime(6) not null,
        transaction_identifier varchar(36) not null,
        primary key (id)
    ) engine=InnoDB;

    create table external_id (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        activity_id varchar(255),
        file_id integer,
        file_unique_id integer,
        guid varchar(255),
        id_type tinyint not null,
        object_identifier varchar(255) not null,
        object_type varchar(255) not null,
        project_id varchar(255) not null,
        wbs varchar(255),
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table invitation (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        email varchar(255) not null,
        last_sent datetime(6) not null,
        participant_identifier varchar(255) not null,
        project_identifier varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table invitation_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table message (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        content varchar(320),
        topic_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table milestone (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        date date not null,
        description varchar(1000),
        header bit not null,
        name varchar(100) not null,
        position integer,
        type tinyint not null,
        craft_id bigint,
        milestone_list_id bigint,
        project_id bigint not null,
        work_area_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table milestone_list (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        date date not null,
        header bit not null,
        work_area_id_constraint bigint not null,
        project_id bigint not null,
        work_area_id bigint,
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

    create table project (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        category varchar(255),
        client varchar(100),
        deleted bit not null default 0,
        description varchar(1000),
        project_end date not null,
        city varchar(100) not null,
        house_number varchar(10) not null,
        street varchar(100) not null,
        zip_code varchar(10) not null,
        project_number varchar(100) not null,
        project_start date not null,
        title varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;

    create table project_craft (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        color varchar(32) not null,
        name varchar(100) not null,
        position integer,
        project_id bigint not null,
        project_craft_list_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table project_craft_list (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table project_import (
        id bigint not null auto_increment,
        blob_name varchar(255) not null,
        craft_column varchar(255),
        craft_column_field_type varchar(255),
        created_date datetime(6) not null,
        job_id varchar(255),
        project_identifier varchar(255) not null,
        read_working_areas_hierarchically bit,
        status varchar(255) not null,
        version bigint not null,
        work_area_column varchar(255),
        work_area_column_field_type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table project_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table project_participant (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        email varchar(255),
        role integer not null,
        status integer not null,
        company_id bigint,
        project_id bigint not null,
        user_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table project_picture (
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
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table relation (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        critical bit,
        source_identifier varchar(36) not null,
        source_type varchar(30) not null,
        target_identifier varchar(36) not null,
        target_type varchar(30) not null,
        type varchar(30) not null,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table rfv_customization (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        active bit not null,
        rfv_key tinyint not null,
        name varchar(50),
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table task (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        deleted bit not null default 0,
        description varchar(1000),
        edit_date datetime(6),
        location varchar(100),
        name varchar(100) not null,
        status integer not null,
        assignee_id bigint,
        project_id bigint not null,
        project_craft_id bigint not null,
        work_area_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table task_action_selection (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        task_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table task_action_selection_set (
        task_action_selection_id bigint not null,
        action varchar(255) not null,
        primary key (task_action_selection_id, action)
    ) engine=InnoDB;

    create table task_attachment (
        dtype integer not null,
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        capture_date datetime(6),
        file_name varchar(256) not null,
        file_size bigint not null,
        full_available bit,
        image_height bigint,
        image_width bigint,
        small_available bit,
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        message_id bigint,
        task_id bigint not null,
        topic_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table task_constraint_customization (
        id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        active bit not null,
        tsk_con_key tinyint not null,
        name varchar(50),
        created_by_id bigint not null,
        last_modified_by_id bigint not null,
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table task_schedule (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        end_date date,
        start_date date,
        project_id bigint not null,
        task_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table taskschedule_taskscheduleslot (
        day_card_date date not null,
        day_card_id bigint not null,
        taskschedule_id bigint,
        primary key (day_card_id)
    ) engine=InnoDB;

    create table topic (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        criticality varchar(30) not null,
        deleted bit not null default 0,
        description varchar(320),
        task_id bigint not null,
        primary key (id)
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
        created_date datetime(6),
        identifier varchar(255) not null,
        last_modified_date datetime(6),
        version bigint not null,
        admin bit not null,
        user_id varchar(100),
        country varchar(255),
        deleted bit not null,
        email varchar(255),
        first_name varchar(50),
        gender varchar(20),
        last_name varchar(50),
        locale varchar(255),
        locked bit not null,
        position varchar(100),
        registered bit not null,
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

    create table work_area (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        name varchar(100) not null,
        parent varchar(36),
        position integer,
        project_id bigint not null,
        work_area_list_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table work_area_list (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        project_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table workday_configuration_holidays (
        workday_configuration_project_id bigint not null,
        date date not null,
        name varchar(100) not null,
        primary key (workday_configuration_project_id, date, name)
    ) engine=InnoDB;

    create table workday_configuration_working_days (
        workday_configuration_project_id bigint not null,
        working_days varchar(12) not null,
        primary key (workday_configuration_project_id, working_days)
    ) engine=InnoDB;

    create table workday_configuration (
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        allow_work_on_non_working_days bit not null,
        start_of_week varchar(12) not null,
        project_id bigint not null,
        primary key (project_id)
    ) engine=InnoDB;

    alter table company 
       add constraint UK_Company_Identifier unique (identifier);

    alter table craft 
       add constraint UK_Craft_Identifier unique (identifier);

    alter table craft_translation 
       add constraint UK_CRAFT_TRANSLATION_LANG unique (craft_id, locale);

    alter table day_card 
       add constraint UK_DayCard_Identifier unique (identifier);

    alter table employee 
       add constraint UK_Employee_Identifier unique (identifier);

    create index IX_EventOfBusinessTransaction_TidEventProcessor 
       on event_of_business_transaction (transaction_identifier, event_processor_name);

    create index IX_ExternalId_ProjType 
       on external_id (project_id, id_type);

    alter table invitation 
       add constraint UK_Invitation_Identifier unique (identifier);

    alter table invitation 
       add constraint UK_Invitation_Part_Identifier unique (participant_identifier);

    alter table invitation 
       add constraint UK_Invitation_Email unique (project_identifier, email);

    alter table message 
       add constraint UK_Message_Identifier unique (identifier);

    alter table milestone 
       add constraint UK_Milestone_Identifier unique (identifier);

    alter table milestone_list 
       add constraint UK_MilestoneList_Identifier unique (identifier);

    alter table milestone_list 
       add constraint UK_MilestoneList_SlotKey unique (project_id, date, header, work_area_id, work_area_id_constraint);

    alter table profile_picture 
       add constraint UK_ProfilePicture_UserId unique (user_id);

    alter table project 
       add constraint UK_Project_Identifier unique (identifier);

    alter table project_craft 
       add constraint UK_ProjCraftName_ProjCraftProj unique (name, project_id);

    alter table project_craft 
       add constraint UK_ProjCraft_Identifier unique (identifier);

    alter table project_craft_list 
       add constraint UK_ProjectCraftList_Identifier unique (identifier);

    alter table project_craft_list 
       add constraint UK_ProjectCraftList_Project unique (project_id);

    create index IX_ProjectImport_CreatedDate 
       on project_import (created_date);

    alter table project_import 
       add constraint UK_ProjectImport_ProjectIdentifier unique (project_identifier);

    alter table project_participant 
       add constraint UK_ProjPart_Assignment unique (project_id, company_id, user_id);

    alter table project_participant 
       add constraint UK_ProjPart_Identifier unique (identifier);

    alter table project_picture 
       add constraint UK_ProjectPicture_ProjectId unique (project_id);

    alter table relation 
       add constraint UK_Relation_Identifier unique (identifier);

    alter table relation 
       add constraint UK_Relation unique (type, source_identifier, source_type, target_identifier, target_type);

    alter table relation 
       add constraint UK_Relation_Search_All unique (project_id, type, identifier);

    alter table rfv_customization 
       add constraint UK_RfvCust_Identifier unique (identifier);

    alter table task 
       add constraint UK_Task_Identifier unique (identifier);

    alter table task_action_selection 
       add constraint UK_TaskActionSelection_Identifier unique (identifier);

    alter table task_action_selection 
       add constraint UK_TaskActionSelection_Task unique (task_id);

    alter table task_attachment 
       add constraint IX_TaskAttachment_Identifier unique (identifier);

    alter table task_constraint_customization 
       add constraint UK_TskConCust_Identifier unique (identifier);

    alter table task_schedule 
       add constraint UK_TaskSchedule_Identifier unique (identifier);

    alter table task_schedule 
       add constraint UK_TaskSchedule_TaskIdentifier unique (task_id);

    alter table topic 
       add constraint UK_Topic_Identifier unique (identifier);

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

    alter table work_area 
       add constraint UK_WorkAreaName_Project_Parent unique (name, project_id, parent);

    alter table work_area 
       add constraint UK_WorkArea_Identifier unique (identifier);

    alter table work_area_list 
       add constraint UK_WorkAreaList_Identifier unique (identifier);

    alter table work_area_list 
       add constraint UK_WorkAreaList_Project unique (project_id);

    alter table workday_configuration 
       add constraint UK_WorkdayConfiguration_Identifier unique (identifier);

    alter table company 
       add constraint FK_Company_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table company 
       add constraint FK_Company_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

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

    alter table day_card 
       add constraint FK_DayCard_TaskSchedule 
       foreign key (task_schedule_id) 
       references task_schedule (id);

    alter table employee 
       add constraint FK_Employee_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table employee 
       add constraint FK_Employee_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table employee 
       add constraint FK_Employee_Company 
       foreign key (company_id) 
       references company (id);

    alter table employee 
       add constraint FK_Employee_User 
       foreign key (user_id) 
       references user_entity (id);

    alter table employee_role 
       add constraint FK_Employee_Role_EmployeeId 
       foreign key (employee_id) 
       references employee (id);

    alter table external_id 
       add constraint FK_ExternalId_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table external_id 
       add constraint FK_ExternalId_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table message 
       add constraint FK_Message_Topic 
       foreign key (topic_id) 
       references topic (id);

    alter table milestone 
       add constraint FK_Milestone_Craft 
       foreign key (craft_id) 
       references project_craft (id);

    alter table milestone 
       add constraint FK_Milestone_MilestoneList 
       foreign key (milestone_list_id) 
       references milestone_list (id);

    alter table milestone 
       add constraint FK_Milestone_Project 
       foreign key (project_id) 
       references project (id);

    alter table milestone 
       add constraint FK_Milestone_Workarea 
       foreign key (work_area_id) 
       references work_area (id);

    alter table milestone_list 
       add constraint FK_MilestoneList_Project 
       foreign key (project_id) 
       references project (id);

    alter table milestone_list 
       add constraint FK_MilestoneList_Workarea 
       foreign key (work_area_id) 
       references work_area (id);

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

    alter table project_craft 
       add constraint FK_ProjCraft_Project 
       foreign key (project_id) 
       references project (id);

    alter table project_craft 
       add constraint FK_ProjectCraft_ProjectCraftList 
       foreign key (project_craft_list_id) 
       references project_craft_list (id);

    alter table project_craft_list 
       add constraint FK_ProjectCraftList_Project 
       foreign key (project_id) 
       references project (id);

    alter table project_participant 
       add constraint FK_ProjPart_Company 
       foreign key (company_id) 
       references company (id);

    alter table project_participant 
       add constraint FK_ProjPart_Project 
       foreign key (project_id) 
       references project (id);

    alter table project_participant 
       add constraint FK_ProjPart_User 
       foreign key (user_id) 
       references user_entity (id);

    alter table project_picture 
       add constraint FK_ProjectPicture_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table project_picture 
       add constraint FK_ProjectPicture_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table project_picture 
       add constraint FK_ProjectPicture_Project 
       foreign key (project_id) 
       references project (id);

    alter table relation 
       add constraint FK_Relation_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table relation 
       add constraint FK_Relation_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table relation 
       add constraint FK_Relation_Project 
       foreign key (project_id) 
       references project (id);

    alter table rfv_customization 
       add constraint FK_RfvCust_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table rfv_customization 
       add constraint FK_RfvCust_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table rfv_customization 
       add constraint FK_RfvCust_Project 
       foreign key (project_id) 
       references project (id);

    alter table task 
       add constraint FK_Task_Assignee 
       foreign key (assignee_id) 
       references project_participant (id);

    alter table task 
       add constraint FK_Task_Project 
       foreign key (project_id) 
       references project (id);

    alter table task 
       add constraint FK_Task_Craft 
       foreign key (project_craft_id) 
       references project_craft (id);

    alter table task 
       add constraint FK_Task_WorkArea 
       foreign key (work_area_id) 
       references work_area (id);

    alter table task_action_selection 
       add constraint FK_TaskActionSelection_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table task_action_selection 
       add constraint FK_TaskActionSelection_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table task_action_selection 
       add constraint FK_TaskActionSelection_Task 
       foreign key (task_id) 
       references task (id);

    alter table task_action_selection_set 
       add constraint FK_TaskActionSelectionSet_TaskActionSelection 
       foreign key (task_action_selection_id) 
       references task_action_selection (id);

    alter table task_attachment 
       add constraint FK_TaskAttachment_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table task_attachment 
       add constraint FK_TaskAttachment_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table task_attachment 
       add constraint FK_TaskAttachment_Message 
       foreign key (message_id) 
       references message (id);

    alter table task_attachment 
       add constraint FK_TaskAttachment_Task 
       foreign key (task_id) 
       references task (id);

    alter table task_attachment 
       add constraint FK_TaskAttachment_Topic 
       foreign key (topic_id) 
       references topic (id);

    alter table task_constraint_customization 
       add constraint FK_TskConCust_CreatedBy 
       foreign key (created_by_id) 
       references user_entity (id);

    alter table task_constraint_customization 
       add constraint FK_TskConCust_LastModifiedBy 
       foreign key (last_modified_by_id) 
       references user_entity (id);

    alter table task_constraint_customization 
       add constraint FK_TskConCust_Project 
       foreign key (project_id) 
       references project (id);

    alter table task_schedule 
       add constraint FK_TaskSchedule_Project 
       foreign key (project_id) 
       references project (id);

    alter table task_schedule 
       add constraint FK_TaskSchedule_Task 
       foreign key (task_id) 
       references task (id);

    alter table taskschedule_taskscheduleslot 
       add constraint FK_TaskScheduleSlot_DayCard_DayCardId 
       foreign key (day_card_id) 
       references day_card (id);

    alter table taskschedule_taskscheduleslot 
       add constraint FK_TaskSchedule_TaskScheduleSlot_TaskScheduleId 
       foreign key (taskschedule_id) 
       references task_schedule (id);

    alter table topic 
       add constraint FK_Topic_Task 
       foreign key (task_id) 
       references task (id);

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

    alter table work_area 
       add constraint FK_WorkArea_Project 
       foreign key (project_id) 
       references project (id);

    alter table work_area 
       add constraint FK_WorkArea_WorkAreaList 
       foreign key (work_area_list_id) 
       references work_area_list (id);

    alter table work_area_list 
       add constraint FK_WorkAreaList_Project 
       foreign key (project_id) 
       references project (id);

    alter table workday_configuration_holidays 
       add constraint FK_WorkdayConfiguration_Holidays 
       foreign key (workday_configuration_project_id) 
       references workday_configuration (project_id);

    alter table workday_configuration_working_days 
       add constraint FK_WorkdayConfiguration_WorkingDays 
       foreign key (workday_configuration_project_id) 
       references workday_configuration (project_id);

    alter table workday_configuration 
       add constraint FK_WorkdayConfiguration_Project 
       foreign key (project_id) 
       references project (id);

create index IX_Compny_CreaBy on company (created_by_id);

create index IX_Compny_LastModiBy on company (last_modified_by_id);

create index IX_Craf_CreaBy on craft (created_by_id);

create index IX_Craf_LastModiBy on craft (last_modified_by_id);

create index IX_CrafTran_Craf on craft_translation (craft_id);

create index IX_DayCard_TaskSche on day_card (task_schedule_id);

create index IX_Emplyee_CreaBy on employee (created_by_id);

create index IX_Emplyee_LastModiBy on employee (last_modified_by_id);

create index IX_Emplyee_Compny on employee (company_id);

create index IX_Emplyee_User on employee (user_id);

create index IX_EmplRole_Emplyee on employee_role (employee_id);

create index IX_Extenal_CreaBy on external_id (created_by_id);

create index IX_Extenal_LastModiBy on external_id (last_modified_by_id);

create index IX_Messge_Topi on message (topic_id);

create index IX_Miletone_Craf on milestone (craft_id);

create index IX_Miletone_MileList on milestone (milestone_list_id);

create index IX_Miletone_Projct on milestone (project_id);

create index IX_Miletone_WorkArea on milestone (work_area_id);

create index IX_MileList_Projct on milestone_list (project_id);

create index IX_MileList_WorkArea on milestone_list (work_area_id);

create index IX_ProfPict_CreaBy on profile_picture (created_by_id);

create index IX_ProfPict_LastModiBy on profile_picture (last_modified_by_id);

create index IX_ProjCraf_Projct on project_craft (project_id);

create index IX_ProjCraf_ProjCrafList on project_craft (project_craft_list_id);

create index IX_ProjPart_Compny on project_participant (company_id);

create index IX_ProjPart_Projct on project_participant (project_id);

create index IX_ProjPart_User on project_participant (user_id);

create index IX_ProjPict_CreaBy on project_picture (created_by_id);

create index IX_ProjPict_LastModiBy on project_picture (last_modified_by_id);

create index IX_Relaion_CreaBy on relation (created_by_id);

create index IX_Relaion_LastModiBy on relation (last_modified_by_id);

create index IX_Relaion_Projct on relation (project_id);

create index IX_RfvCust_CreaBy on rfv_customization (created_by_id);

create index IX_RfvCust_LastModiBy on rfv_customization (last_modified_by_id);

create index IX_RfvCust_Projct on rfv_customization (project_id);

create index IX_Task_Assinee on task (assignee_id);

create index IX_Task_Projct on task (project_id);

create index IX_Task_ProjCraf on task (project_craft_id);

create index IX_Task_WorkArea on task (work_area_id);

create index IX_TaskActiSele_CreaBy on task_action_selection (created_by_id);

create index IX_TaskActiSele_LastModiBy on task_action_selection (last_modified_by_id);

create index IX_TaskActiSeleSet_TaskActiSele on task_action_selection_set (task_action_selection_id);

create index IX_TaskAtta_CreaBy on task_attachment (created_by_id);

create index IX_TaskAtta_LastModiBy on task_attachment (last_modified_by_id);

create index IX_TaskAtta_Messge on task_attachment (message_id);

create index IX_TaskAtta_Task on task_attachment (task_id);

create index IX_TaskAtta_Topi on task_attachment (topic_id);

create index IX_TaskConsCust_CreaBy on task_constraint_customization (created_by_id);

create index IX_TaskConsCust_LastModiBy on task_constraint_customization (last_modified_by_id);

create index IX_TaskConsCust_Projct on task_constraint_customization (project_id);

create index IX_TaskSche_Projct on task_schedule (project_id);

create index IX_TaskTask_DayCard on taskschedule_taskscheduleslot (day_card_id);

create index IX_TaskTask_Taskdule on taskschedule_taskscheduleslot (taskschedule_id);

create index IX_Topi_Task on topic (task_id);

create index IX_UserCraf_Craf on user_craft (craft_id);

create index IX_UserCraf_User on user_craft (user_id);

create index IX_UserEnti_CreaBy on user_entity (created_by_id);

create index IX_UserEnti_LastModiBy on user_entity (last_modified_by_id);

create index IX_UserPhon_User on user_phonenumber (user_id);

create index IX_WorkArea_Projct on work_area (project_id);

create index IX_WorkArea_WorkAreaList on work_area (work_area_list_id);

create index IX_WorkConfHoli_WorkConfProj on workday_configuration_holidays (workday_configuration_project_id);

create index IX_WorkConfWorkDays_WorkConfProj on workday_configuration_working_days (workday_configuration_project_id);

create index IX_WorkConf_Projct on workday_configuration (project_id);


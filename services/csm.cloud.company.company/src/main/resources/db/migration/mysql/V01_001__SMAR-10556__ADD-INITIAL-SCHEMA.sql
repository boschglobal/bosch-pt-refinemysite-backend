
    create table company (
       id bigint not null auto_increment,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        name varchar(100) not null,
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

    create table company_kafka_event (
       id bigint not null auto_increment,
        event longblob,
        event_key longblob,
        partition_number integer,
        trace_header_key varchar(255),
        trace_header_value varchar(255),
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

    create table user_entity (
       id bigint not null auto_increment,
        created_date datetime(6),
        identifier varchar(255) not null,
        last_modified_date datetime(6),
        version bigint not null,
        admin bit not null,
        deleted bit not null,
        email varchar(255),
        first_name varchar(50),
        gender varchar(20),
        last_name varchar(50),
        position varchar(100),
        registered bit not null,
        user_id varchar(100),
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

    alter table company
       add constraint UK_Company_Identifier unique (identifier);

    alter table employee
       add constraint UK_Employee_Identifier unique (identifier);

    alter table profile_picture
       add constraint UK_ProfilePicture_UserId unique (user_id);

    alter table user_entity
       add constraint UK_UserId unique (user_id);

    alter table user_entity
       add constraint UK_User_Identifier unique (identifier);

    alter table user_entity
       add constraint UK_User_Email unique (email);

    alter table company
       add constraint FK_Company_CreatedBy
       foreign key (created_by_id)
       references user_entity (id);

    alter table company
       add constraint FK_Company_LastModifiedBy
       foreign key (last_modified_by_id)
       references user_entity (id);

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

create index IX_Compny_CreaBy on company (created_by_id);

create index IX_Compny_LastModiBy on company (last_modified_by_id);

create index IX_Emplyee_CreaBy on employee (created_by_id);

create index IX_Emplyee_LastModiBy on employee (last_modified_by_id);

create index IX_Emplyee_Compny on employee (company_id);

create index IX_Emplyee_User on employee (user_id);

create index IX_EmplRole_Emplyee on employee_role (employee_id);

create index IX_ProfPict_CreaBy on profile_picture (created_by_id);

create index IX_ProfPict_LastModiBy on profile_picture (last_modified_by_id);

create index IX_UserEnti_CreaBy on user_entity (created_by_id);

create index IX_UserEnti_LastModiBy on user_entity (last_modified_by_id);

create index IX_UserPhon_User on user_phonenumber (user_id);


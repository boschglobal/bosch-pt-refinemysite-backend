
    create table company (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
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
        primary key (id)
    ) engine=InnoDB;

    create table company_kafka_event (
        id bigint not null auto_increment,
        event mediumblob,
        event_key mediumblob not null,
        partition_number integer not null,
        trace_header_key varchar(255) not null,
        trace_header_value varchar(255) not null,
        transaction_identifier varchar(36),
        primary key (id)
    ) engine=InnoDB;

    create table employee (
        id bigint not null auto_increment,
        created_by varchar(36) not null,
        created_date datetime(6) not null,
        identifier varchar(36) not null,
        last_modified_by varchar(36) not null,
        last_modified_date datetime(6) not null,
        version bigint not null,
        user_ref varchar(255) not null,
        company_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table employee_role (
        employee_id bigint not null,
        roles varchar(255)
    ) engine=InnoDB;

    create table projection_employable_user (
        identifier varchar(255) not null,
        admin bit not null,
        company_identifier varchar(255),
        company_name varchar(255),
        email varchar(255),
        employee_created_date datetime(6),
        employee_identifier varchar(255),
        first_name varchar(255),
        gender varchar(255),
        last_name varchar(255),
        locked bit not null,
        user_country varchar(255),
        user_created_date datetime(6),
        user_name varchar(255),
        primary key (identifier)
    ) engine=InnoDB;

    create table projection_employable_user_company_name (
        identifier varchar(255) not null,
        company_name varchar(255) not null,
        primary key (identifier)
    ) engine=InnoDB;

    create table projection_user (
        identifier varchar(255) not null,
        admin bit not null,
        ciam_user_identifier varchar(255) not null,
        country varchar(255),
        created_by varchar(255) not null,
        created_date datetime(6),
        email varchar(255) not null,
        first_name varchar(255) not null,
        last_modified_by varchar(255) not null,
        last_modified_date datetime(6),
        last_name varchar(255) not null,
        locale varchar(255),
        locked bit not null,
        version bigint not null,
        primary key (identifier)
    ) engine=InnoDB;

    create table user_country_restriction (
        id bigint not null auto_increment,
        country_code varchar(255) not null,
        user_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table company 
       add constraint UK_Company_Identifier unique (identifier);

    create index IX_Employee_UserRef 
       on employee (user_ref);

    alter table employee 
       add constraint UK_Employee_Identifier unique (identifier);

    create index IX_UserName_CompanyName 
       on projection_employable_user (user_name, company_name);

    create index IX_CompanyName_UserName 
       on projection_employable_user (company_name, user_name);

    alter table projection_employable_user 
       add constraint UK_Email unique (email);

    alter table projection_user 
       add constraint UK_Email unique (email);

    alter table projection_user 
       add constraint UK_CiamUserIdentifier unique (ciam_user_identifier);

    create index IX_UserCountryRestriction_User 
       on user_country_restriction (user_id);

    alter table user_country_restriction 
       add constraint UK_UserCountryRestriction_User_Country unique (user_id, country_code);

    alter table employee 
       add constraint FK_Employee_Company 
       foreign key (company_id) 
       references company (id);

    alter table employee_role 
       add constraint FK_Employee_Role_EmployeeId 
       foreign key (employee_id) 
       references employee (id);

create index IX_Emplyee_Compny on employee (company_id);

create index IX_EmplRole_Emplyee on employee_role (employee_id);


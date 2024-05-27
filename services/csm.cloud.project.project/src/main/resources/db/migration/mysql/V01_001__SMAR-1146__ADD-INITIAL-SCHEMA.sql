create table address_postbox (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  area                varchar(100),
  city                varchar(100) not null,
  country             varchar(100),
  zip_code            varchar(10)  not null,
  post_box            varchar(100),
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table address_street (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  area                varchar(100),
  city                varchar(100) not null,
  country             varchar(100),
  zip_code            varchar(10)  not null,
  house_number        varchar(10),
  street              varchar(100),
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table company (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  name                varchar(100) not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  post_box_address_id bigint,
  street_address_id   bigint,
  primary key (id)
)
  engine = InnoDB;

create table company_kafka_event_store (
  id               bigint not null auto_increment,
  event            longblob,
  event_key        longblob,
  partition_number integer,
  primary key (id)
)
  engine = InnoDB;

create table craft (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  default_name        varchar(128) not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table craft_translation (
  craft_id bigint       not null,
  locale   varchar(255) not null,
  value    varchar(255) not null
)
  engine = InnoDB;

create table craft_kafka_event_store (
  id               bigint not null auto_increment,
  event            longblob,
  event_key        longblob,
  partition_number integer,
  primary key (id)
)
  engine = InnoDB;

create table day_card (
  id                  bigint         not null auto_increment,
  created_date        datetime(6)    not null,
  identifier          varchar(255)   not null,
  last_modified_date  datetime(6)    not null,
  version             bigint         not null,
  manpower            decimal(19, 2) not null,
  notes               varchar(500),
  reason              varchar(30),
  status              varchar(10)    not null,
  title               varchar(100)   not null,
  created_by_id       bigint         not null,
  last_modified_by_id bigint         not null,
  task_schedule_id    bigint         not null,
  primary key (id)
)
  engine = InnoDB;

create table employee (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  company_id          bigint       not null,
  user_id             bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table employee_role (
  employee_id bigint not null,
  roles       varchar(255)
)
  engine = InnoDB;

create table message (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  content             varchar(320),
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  topic_id            bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table profile_picture (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  file_size           bigint       not null,
  full_available      bit,
  height              bigint       not null,
  small_available     bit,
  width               bigint       not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  user_id             bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table project (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  category            varchar(255),
  client              varchar(100),
  description         varchar(1000),
  project_end         date         not null,
  city                varchar(100) not null,
  house_number        varchar(10)  not null,
  street              varchar(100) not null,
  zip_code            varchar(10)  not null,
  project_number      varchar(100) not null,
  project_start       date         not null,
  title               varchar(100) not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table project_craft (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  color               varchar(32)  not null,
  name                varchar(100) not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  project_id          bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table project_kafka_event_store (
  id               bigint not null auto_increment,
  event            longblob,
  event_key        longblob,
  partition_number integer,
  primary key (id)
)
  engine = InnoDB;

create table project_participant (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  role                integer      not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  employee_id         bigint       not null,
  project_id          bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table project_picture (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  file_size           bigint       not null,
  full_available      bit,
  height              bigint       not null,
  small_available     bit,
  width               bigint       not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  project_id          bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table task (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  description         varchar(1000),
  edit_date           datetime(6),
  location            varchar(100),
  name                varchar(100) not null,
  status              integer      not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  assignee_id         bigint,
  project_id          bigint       not null,
  project_craft_id    bigint       not null,
  work_area_id        bigint,
  primary key (id)
)
  engine = InnoDB;

create table task_action_selection (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  task_id             bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table task_action_selection_set (
  task_action_selection_id bigint       not null,
  action                   varchar(255) not null,
  primary key (task_action_selection_id, action)
)
  engine = InnoDB;

create table task_attachment (
  dtype               integer      not null,
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  capture_date        datetime(6),
  file_name           varchar(256) not null,
  file_size           bigint       not null,
  full_available      bit,
  image_height        bigint,
  image_width         bigint,
  small_available     bit,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  message_id          bigint,
  task_id             bigint       not null,
  topic_id            bigint,
  primary key (id)
)
  engine = InnoDB;

create table task_schedule (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  end_date            date,
  start_date          date,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  task_id             bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table taskschedule_taskscheduleslot (
  taskschedule_id bigint not null,
  day_card_date   date   not null,
  day_card_id     bigint not null
)
  engine = InnoDB;

create table topic (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  criticality         varchar(30)  not null,
  description         varchar(320),
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  task_id             bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table user_craft (
  user_id  bigint not null,
  craft_id bigint not null
)
  engine = InnoDB;

create table user_entity (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  admin               bit          not null,
  email               varchar(255) not null,
  first_name          varchar(50),
  gender              varchar(20),
  last_name           varchar(50),
  position            varchar(100),
  registered          bit          not null,
  user_id             varchar(100) not null,
  created_by_id       bigint,
  last_modified_by_id bigint,
  primary key (id)
)
  engine = InnoDB;

create table user_phonenumber (
  user_id           bigint       not null,
  call_number       varchar(25)  not null,
  country_code      varchar(5)   not null,
  phone_number_type varchar(255) not null
)
  engine = InnoDB;

create table user_kafka_event_store (
  id               bigint not null auto_increment,
  event            longblob,
  event_key        longblob,
  partition_number integer,
  primary key (id)
)
  engine = InnoDB;

create table work_area (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  name                varchar(100) not null,
  position            integer      not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  work_area_list_id   bigint       not null,
  primary key (id)
)
  engine = InnoDB;

create table work_area_list (
  id                  bigint       not null auto_increment,
  created_date        datetime(6)  not null,
  identifier          varchar(255) not null,
  last_modified_date  datetime(6)  not null,
  version             bigint       not null,
  created_by_id       bigint       not null,
  last_modified_by_id bigint       not null,
  project_id          bigint       not null,
  primary key (id)
)
  engine = InnoDB;

alter table address_postbox
  add constraint UK_AddressPostBox_Identifier unique (identifier);

alter table address_street
  add constraint UK_AddressStreet_Identifier unique (identifier);

alter table company
  add constraint UK_Company_Identifier unique (identifier);

alter table craft
  add constraint UK_Craft_Identifier unique (identifier);

alter table craft_translation
  add constraint UK_CRAFT_TRANSLATION_LANG unique (craft_id, locale);

alter table day_card
  add constraint UK_DayCard_Identifier unique (identifier);

alter table employee
  add constraint UK_Employee_User unique (user_id);

alter table employee
  add constraint UK_Employee_Identifier unique (identifier);

alter table message
  add constraint UK_Message_Identifier unique (identifier);

alter table profile_picture
  add constraint UK_ProfilePicture_UserId unique (user_id);

alter table project
  add constraint UK_Project_Identifier unique (identifier);

alter table project_craft
  add constraint UK_ProjCraftName_ProjCraftProj unique (name, project_id);

alter table project_craft
  add constraint UK_ProjCraft_Identifier unique (identifier);

alter table project_participant
  add constraint UK_ProjPart_ProjEmpl unique (project_id, employee_id);

alter table project_picture
  add constraint UK_ProjectPicture_ProjectId unique (project_id);

alter table task
  add constraint UK_Task_Identifier unique (identifier);

alter table task_action_selection
  add constraint UK_TaskActionSelection_Identifier unique (identifier);

alter table task_action_selection
  add constraint UK_TaskActionSelection_Task unique (task_id);

alter table task_attachment
  add constraint IX_TaskAttachment_Identifier unique (identifier);

alter table task_schedule
  add constraint UK_TaskSchedule_Identifier unique (identifier);

alter table task_schedule
  add constraint UK_TaskSchedule_TaskIdentifier unique (task_id);

alter table taskschedule_taskscheduleslot
  add constraint UK_TaskScheduleSlot_DayCard unique (day_card_id);

alter table topic
  add constraint UK_Topic_Identifier unique (identifier);

alter table user_entity
  add constraint UK_UserId unique (user_id);

alter table user_entity
  add constraint UK_User_Identifier unique (identifier);

alter table user_entity
  add constraint UK_User_Email unique (email);

alter table work_area
  add constraint UK_WorkAreaName_WorkAreaList unique (name, work_area_list_id);

alter table work_area
  add constraint UK_WorkArea_Identifier unique (identifier);

alter table work_area_list
  add constraint UK_WorkAreaList_Identifier unique (identifier);

alter table work_area_list
  add constraint UK_WorkAreaList_Project unique (project_id);

alter table address_postbox
  add constraint FK_PostBox_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table address_postbox
  add constraint FK_PostBox_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table address_street
  add constraint FK_Street_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table address_street
  add constraint FK_Street_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table company
  add constraint FK_Company_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table company
  add constraint FK_Company_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table company
  add constraint FK_PostBoxAddress
foreign key (post_box_address_id)
references address_postbox (id);

alter table company
  add constraint FK_StreetAddress
foreign key (street_address_id)
references address_street (id);

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
  add constraint FK_DayCard_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table day_card
  add constraint FK_DayCard_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

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

alter table message
  add constraint FK_Message_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table message
  add constraint FK_Message_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table message
  add constraint FK_Message_Topic
foreign key (topic_id)
references topic (id);

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

alter table project
  add constraint FK_Project_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table project
  add constraint FK_Project_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table project_craft
  add constraint FK_ProjCraft_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table project_craft
  add constraint FK_ProjCraft_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table project_craft
  add constraint FK_ProjCraft_Project
foreign key (project_id)
references project (id);

alter table project_participant
  add constraint FK_ProjPart_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table project_participant
  add constraint FK_ProjPart_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table project_participant
  add constraint FK_ProjPart_Employee
foreign key (employee_id)
references employee (id);

alter table project_participant
  add constraint FK_ProjPart_Project
foreign key (project_id)
references project (id);

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

alter table task
  add constraint FK_Task_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table task
  add constraint FK_Task_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

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

alter table task_schedule
  add constraint FK_TaskSchedule_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table task_schedule
  add constraint FK_TaskSchedule_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

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
  add constraint FK_Topic_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table topic
  add constraint FK_Topic_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

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
  add constraint FK_WorkArea_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table work_area
  add constraint FK_WorkArea_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table work_area
  add constraint FK_WorkArea_WorkAreaList
foreign key (work_area_list_id)
references work_area_list (id);

alter table work_area_list
  add constraint FK_WorkAreaList_CreatedBy
foreign key (created_by_id)
references user_entity (id);

alter table work_area_list
  add constraint FK_WorkAreaList_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id);

alter table work_area_list
  add constraint FK_WorkAreaList_Project
foreign key (project_id)
references project (id);

create index IX_AddrPost_CreaBy
  on address_postbox (created_by_id);

create index IX_AddrPost_LastModiBy
  on address_postbox (last_modified_by_id);

create index IX_AddrStre_CreaBy
  on address_street (created_by_id);

create index IX_AddrStre_LastModiBy
  on address_street (last_modified_by_id);

create index IX_Compny_CreaBy
  on company (created_by_id);

create index IX_Compny_LastModiBy
  on company (last_modified_by_id);

create index IX_Compny_PostBoxAddr
  on company (post_box_address_id);

create index IX_Compny_StreAddr
  on company (street_address_id);

create index IX_Craf_CreaBy
  on craft (created_by_id);

create index IX_Craf_LastModiBy
  on craft (last_modified_by_id);

create index IX_CrafTran_Craf
  on craft_translation (craft_id);

create index IX_DayCard_CreaBy
  on day_card (created_by_id);

create index IX_DayCard_LastModiBy
  on day_card (last_modified_by_id);

create index IX_DayCard_TaskSche
  on day_card (task_schedule_id);

create index IX_Emplyee_CreaBy
  on employee (created_by_id);

create index IX_Emplyee_LastModiBy
  on employee (last_modified_by_id);

create index IX_Emplyee_Compny
  on employee (company_id);

create index IX_EmplRole_Emplyee
  on employee_role (employee_id);

create index IX_Messge_CreaBy
  on message (created_by_id);

create index IX_Messge_LastModiBy
  on message (last_modified_by_id);

create index IX_Messge_Topi
  on message (topic_id);

create index IX_ProfPict_CreaBy
  on profile_picture (created_by_id);

create index IX_ProfPict_LastModiBy
  on profile_picture (last_modified_by_id);

create index IX_Projct_CreaBy
  on project (created_by_id);

create index IX_Projct_LastModiBy
  on project (last_modified_by_id);

create index IX_ProjCraf_CreaBy
  on project_craft (created_by_id);

create index IX_ProjCraf_LastModiBy
  on project_craft (last_modified_by_id);

create index IX_ProjCraf_Projct
  on project_craft (project_id);

create index IX_ProjPart_CreaBy
  on project_participant (created_by_id);

create index IX_ProjPart_LastModiBy
  on project_participant (last_modified_by_id);

create index IX_ProjPart_Emplyee
  on project_participant (employee_id);

create index IX_ProjPart_Projct
  on project_participant (project_id);

create index IX_ProjPict_CreaBy
  on project_picture (created_by_id);

create index IX_ProjPict_LastModiBy
  on project_picture (last_modified_by_id);

create index IX_Task_CreaBy
  on task (created_by_id);

create index IX_Task_LastModiBy
  on task (last_modified_by_id);

create index IX_Task_Assinee
  on task (assignee_id);

create index IX_Task_Projct
  on task (project_id);

create index IX_Task_ProjCraf
  on task (project_craft_id);

create index IX_Task_WorkArea
  on task (work_area_id);

create index IX_TaskActiSele_CreaBy
  on task_action_selection (created_by_id);

create index IX_TaskActiSele_LastModiBy
  on task_action_selection (last_modified_by_id);

create index IX_TaskActiSeleSet_TaskActiSele
  on task_action_selection_set (task_action_selection_id);

create index IX_TaskAtta_CreaBy
  on task_attachment (created_by_id);

create index IX_TaskAtta_LastModiBy
  on task_attachment (last_modified_by_id);

create index IX_TaskAtta_Messge
  on task_attachment (message_id);

create index IX_TaskAtta_Task
  on task_attachment (task_id);

create index IX_TaskAtta_Topi
  on task_attachment (topic_id);

create index IX_TaskSche_CreaBy
  on task_schedule (created_by_id);

create index IX_TaskSche_LastModiBy
  on task_schedule (last_modified_by_id);

create index IX_TaskTask_Taskdule
  on taskschedule_taskscheduleslot (taskschedule_id);

create index IX_Topi_CreaBy
  on topic (created_by_id);

create index IX_Topi_LastModiBy
  on topic (last_modified_by_id);

create index IX_Topi_Task
  on topic (task_id);

create index IX_UserCraf_Craf
  on user_craft (craft_id);

create index IX_UserCraf_User
  on user_craft (user_id);

create index IX_UserEnti_CreaBy
  on user_entity (created_by_id);

create index IX_UserEnti_LastModiBy
  on user_entity (last_modified_by_id);

create index IX_UserPhon_User
  on user_phonenumber (user_id);

create index IX_WorkArea_CreaBy
  on work_area (created_by_id);

create index IX_WorkArea_LastModiBy
  on work_area (last_modified_by_id);

create index IX_WorkArea_WorkAreaList
  on work_area (work_area_list_id);

create index IX_WorkAreaList_CreaBy
  on work_area_list (created_by_id);

create index IX_WorkAreaList_LastModiBy
  on work_area_list (last_modified_by_id);


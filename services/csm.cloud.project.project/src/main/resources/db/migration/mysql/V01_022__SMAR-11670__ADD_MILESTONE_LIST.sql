alter table milestone
    add column position integer;

alter table milestone
    add column milestone_list_id bigint;

create table milestone_list
(
    id                  bigint       not null auto_increment,
    created_date        datetime(6)  not null,
    identifier          varchar(255) not null,
    last_modified_date  datetime(6)  not null,
    version             bigint       not null,
    date                date         not null,
    header              bit          not null,
    created_by_id       bigint       not null,
    last_modified_by_id bigint       not null,
    project_id          bigint       not null,
    work_area_id        bigint,
    primary key (id)
) engine = InnoDB;

alter table milestone_list
    add constraint UK_MilestoneList_Identifier unique (identifier);

alter table milestone_list
    add constraint UK_MilestoneList_SlotKey unique (project_id, date, header, work_area_id);

alter table milestone
    add constraint FK_Milestone_MilestoneList
        foreign key (milestone_list_id)
            references milestone_list (id);

alter table milestone_list
    add constraint FK_MilestoneList_CreatedBy
        foreign key (created_by_id)
            references user_entity (id);

alter table milestone_list
    add constraint FK_MilestoneList_LastModifiedBy
        foreign key (last_modified_by_id)
            references user_entity (id);

alter table milestone_list
    add constraint FK_MilestoneList_Project
        foreign key (project_id)
            references project (id);

alter table milestone_list
    add constraint FK_MilestoneList_Workarea
        foreign key (work_area_id)
            references work_area (id);

create index IX_Miletone_MileList on milestone (milestone_list_id);

create index IX_MileList_CreaBy on milestone_list (created_by_id);

create index IX_MileList_LastModiBy on milestone_list (last_modified_by_id);

create index IX_MileList_Projct on milestone_list (project_id);

create index IX_MileList_WorkArea on milestone_list (work_area_id);
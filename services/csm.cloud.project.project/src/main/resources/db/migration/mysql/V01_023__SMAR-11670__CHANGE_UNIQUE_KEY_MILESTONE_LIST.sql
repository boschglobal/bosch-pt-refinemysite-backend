alter table milestone_list
    drop index UK_MilestoneList_SlotKey;

alter table milestone_list
    add column work_area_id_constraint varchar(255);

update milestone_list ml
    left join work_area wa on ml.work_area_id = wa.id
set ml.work_area_id_constraint = case when ml.work_area_id is null then -1 else wa.id end;

alter table milestone_list
    modify work_area_id_constraint varchar(255) not null;

alter table milestone_list
    add constraint UK_MilestoneList_SlotKey unique (project_id, date, header, work_area_id_constraint);
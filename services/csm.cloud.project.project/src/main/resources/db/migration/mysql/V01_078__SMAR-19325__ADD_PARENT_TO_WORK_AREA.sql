alter table work_area
    add column parent varchar(36);

drop index UK_WorkAreaName_Project on work_area;

alter table work_area
    add constraint UK_WorkAreaName_Project_Parent unique (name, project_id, parent);
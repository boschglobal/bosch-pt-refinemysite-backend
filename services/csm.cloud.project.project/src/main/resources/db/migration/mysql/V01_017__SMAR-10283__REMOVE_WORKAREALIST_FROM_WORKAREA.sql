-- drop not null constraint of work_area_list_id and position column
alter table work_area modify work_area_list_id bigint null;
alter table work_area modify position integer null;

-- add project column, set values and add not null constraint
alter table work_area
  add column project_id bigint;

update work_area wa
 inner join work_area_list wal on wa.work_area_list_id = wal.id
 set wa.project_id = wal.project_id;

alter table work_area modify project_id bigint not null;

-- remove unique index of workarea-list + name tuple (requires that the work area list exists)
alter table work_area drop index UK_WorkAreaName_WorkAreaList;

-- add constraints and indices
alter table work_area
  add constraint UK_WorkAreaName_Project unique (name, project_id);

alter table work_area
  add constraint FK_WorkArea_Project
  foreign key (project_id)
  references project (id);

create index IX_WorkArea_Projct on work_area (project_id);
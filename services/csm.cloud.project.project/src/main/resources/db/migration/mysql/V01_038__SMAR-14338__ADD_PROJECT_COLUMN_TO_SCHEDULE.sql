alter table task_schedule
    add column project_id bigint;

update task_schedule s join task t
on t.id = s.task_id
    set s.project_id = t.project_id;

alter table task_schedule
    modify project_id bigint not null;

create index IX_TaskSche_Projct on task_schedule (project_id);

alter table task_schedule
    add constraint FK_TaskSchedule_Project foreign key (project_id) references project (id);


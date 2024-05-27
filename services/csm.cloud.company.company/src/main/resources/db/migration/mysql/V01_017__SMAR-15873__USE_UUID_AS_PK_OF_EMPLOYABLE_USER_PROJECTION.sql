SET FOREIGN_KEY_CHECKS = 0;
alter table projection_employable_user
    modify id bigint not null;
SET FOREIGN_KEY_CHECKS = 1;

alter table projection_employable_user
    drop primary key,
    add primary key (identifier);

alter table projection_employable_user
    drop column id;
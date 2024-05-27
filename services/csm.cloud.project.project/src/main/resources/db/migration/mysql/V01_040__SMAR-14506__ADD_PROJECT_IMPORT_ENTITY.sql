create table project_import
(
    id                                bigint       not null auto_increment,
    blob_name                         varchar(255) not null,
    craft_column                      varchar(255),
    created_date                      datetime(6) not null,
    job_id                            varchar(255),
    project_identifier                varchar(255) not null,
    read_working_areas_hierarchically bit,
    status                            varchar(255) not null,
    version                           bigint       not null,
    work_area_column                  varchar(255),
    primary key (id)
) engine=InnoDB;

create index IX_ProjectImport_CreatedDate on project_import (created_date);

alter table project_import
    add constraint UK_ProjectImport_ProjectIdentifier unique (project_identifier);

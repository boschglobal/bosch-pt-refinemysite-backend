ALTER TABLE invitation MODIFY identifier varchar (36) not null;
ALTER TABLE invitation MODIFY created_by varchar (36) not null;
ALTER TABLE invitation MODIFY last_modified_by varchar (36) not null;

ALTER TABLE project_participant MODIFY identifier varchar (36) not null;
ALTER TABLE project_participant MODIFY created_by varchar (36) not null;
ALTER TABLE project_participant MODIFY last_modified_by varchar (36) not null;
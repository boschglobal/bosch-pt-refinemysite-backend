ALTER TABLE company MODIFY identifier varchar (36) not null;
ALTER TABLE company MODIFY created_by varchar (36) not null;
ALTER TABLE company MODIFY last_modified_by varchar (36) not null;

ALTER TABLE employee MODIFY identifier varchar (36) not null;
ALTER TABLE employee MODIFY created_by varchar (36) not null;
ALTER TABLE employee MODIFY last_modified_by varchar (36) not null;
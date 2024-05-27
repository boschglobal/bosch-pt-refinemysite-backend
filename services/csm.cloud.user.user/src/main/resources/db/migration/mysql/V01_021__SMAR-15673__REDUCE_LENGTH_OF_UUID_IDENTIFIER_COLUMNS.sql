ALTER TABLE user_entity MODIFY identifier varchar (36) not null;
ALTER TABLE user_entity MODIFY created_by varchar (36) not null;
ALTER TABLE user_entity MODIFY last_modified_by varchar (36) not null;

ALTER TABLE profile_picture MODIFY identifier varchar (36) not null;
ALTER TABLE profile_picture MODIFY created_by varchar (36) not null;
ALTER TABLE profile_picture MODIFY last_modified_by varchar (36) not null;

ALTER TABLE craft MODIFY identifier varchar (36) not null;
ALTER TABLE craft MODIFY created_by varchar (36) not null;
ALTER TABLE craft MODIFY last_modified_by varchar (36) not null;

ALTER TABLE consents_user MODIFY identifier varchar (36) not null;
ALTER TABLE consents_user MODIFY created_by varchar (36) not null;
ALTER TABLE consents_user MODIFY last_modified_by varchar (36) not null;

ALTER TABLE document MODIFY identifier varchar (36) not null;
ALTER TABLE document MODIFY created_by varchar (36) not null;
ALTER TABLE document MODIFY last_modified_by varchar (36) not null;
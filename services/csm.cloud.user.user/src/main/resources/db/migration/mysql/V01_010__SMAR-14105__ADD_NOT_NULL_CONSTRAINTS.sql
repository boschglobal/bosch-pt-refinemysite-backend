alter table craft modify created_by varchar(255) not null;

alter table craft modify last_modified_by varchar(255) not null;

alter table profile_picture modify created_by varchar(255) not null;

alter table profile_picture modify last_modified_by varchar(255) not null;

alter table user_entity modify created_by varchar(255) not null;

alter table user_entity modify last_modified_by varchar(255) not null;
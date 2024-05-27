alter table user_entity
  drop foreign key FK_User_CreatedBy;

alter table user_entity
  drop foreign key FK_User_LastModifiedBy;

alter table user_entity
  add constraint FK_User_CreatedBy
foreign key (created_by_id)
references user_entity (id)
  on delete set null;

alter table user_entity
  add constraint FK_User_LastModifiedBy
foreign key (last_modified_by_id)
references user_entity (id)
  on delete set null;

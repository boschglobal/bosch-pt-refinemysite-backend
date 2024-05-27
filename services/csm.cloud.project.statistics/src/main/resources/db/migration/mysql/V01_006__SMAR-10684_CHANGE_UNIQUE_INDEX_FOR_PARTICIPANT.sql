alter table participant_mapping
    drop index IX_ParMap_ProjIdenUserIdent;

alter table participant_mapping
    add constraint IX_ParMap_ProjIdCompIdUserId unique (project_identifier, user_identifier, company_identifier);

-- refactor document to a snapshot
alter table document change column display_name title varchar (255) not null;

alter table document
    add column (created_by varchar(255) not null,
        created_date datetime(6) not null,
        identifier varchar(255) not null,
        last_modified_by varchar(255) not null,
        last_modified_date datetime(6) not null,
        version bigint not null);

-- remove foreign key on doc version (for loose coupling)
alter table user_consent
drop
foreign key FK_UserConsent_DocumentVersion;

alter table user_consent drop index IX_UserCons_DocuVers;

alter table user_consent change column document_version_id document_version_id varchar (255) not null;

-- make document version an embeddable
alter table document_version drop column id;

alter table document_version
    add constraint UK_DocumentVersion_Document_LastChanged unique (document_id, last_changed);
create table document
(
    id      bigint       not null auto_increment,
    country varchar(255) not null,
    locale  varchar(255) not null,
    name    varchar(255) not null,
    url     varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table document_version
(
    id           bigint       not null auto_increment,
    identifier   varchar(255) not null,
    last_changed datetime(6) not null,
    document_id  bigint       not null,
    primary key (id)
) engine=InnoDB;

create table user_consent
(
    id                  bigint       not null auto_increment,
    date                date         not null,
    user_identifier     varchar(255) not null,
    document_version_id bigint       not null,
    primary key (id)
) engine=InnoDB;



alter table document
    add constraint UK_Document_Name_Country_Locale unique (name, country, locale);

alter table document_version
    add constraint UK_DocumentVersion_Identifier unique (identifier);

alter table document_version
    add constraint FK_DocumentVersion_Document
        foreign key (document_id)
            references document (id);

alter table user_consent
    add constraint FK_UserConsent_DocumentVersion
        foreign key (document_version_id)
            references document_version (id);

create index IX_UserConsent_UserIdentifier on user_consent (user_identifier);

create index IX_Document_Country_Locale on document (country, locale);

create index IX_DocuVers_Docuent on document_version (document_id);

create index IX_UserCons_DocuVers on user_consent (document_version_id);

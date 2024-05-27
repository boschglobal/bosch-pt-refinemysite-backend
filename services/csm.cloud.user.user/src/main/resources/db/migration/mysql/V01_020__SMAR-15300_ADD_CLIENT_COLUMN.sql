alter table document
    add column client varchar(32);

update document
set client = 'ALL';

alter table document
    change column client client varchar (32) not null;

alter table document drop index UK_Document_Type_Country_Locale;

alter table document
    change column country country varchar (32) not null;

alter table document
    change column document_type document_type varchar (32) not null;

alter table document
    change column locale locale varchar (32) not null;

alter table document
    add constraint UK_Document_Type_Country_Locale_Client unique (document_type, country, locale, client);
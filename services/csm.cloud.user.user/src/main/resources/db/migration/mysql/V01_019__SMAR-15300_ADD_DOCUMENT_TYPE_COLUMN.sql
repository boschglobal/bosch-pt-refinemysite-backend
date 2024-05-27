alter table document
    add column document_type varchar(255);

update document
set document_type = 'TERMS_AND_CONDITIONS';

alter table document
    change column document_type document_type varchar (255) not null;

alter table document drop index UK_Document_Country_Locale;

alter table document
    add constraint UK_Document_Type_Country_Locale unique (document_type, country, locale);
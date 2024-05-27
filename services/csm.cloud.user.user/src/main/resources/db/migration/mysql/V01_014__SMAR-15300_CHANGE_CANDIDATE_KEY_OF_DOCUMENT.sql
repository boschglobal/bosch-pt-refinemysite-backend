drop index IX_Document_Country_Locale on document;

drop index UK_Document_Name_Country_Locale on document;

alter table document
    add constraint UK_Document_Country_Locale unique (country, locale);
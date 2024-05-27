alter table user_consent
    add constraint UK_UserConsent_DocumentVersion_UserIdentifier unique (document_version_id, user_identifier);
-- this migration is not a compatible change if current data is taken into account
--      - user_identifier is now identified by user_identifier of consents_user
--      - added foreign key column needs to match to the corresponding id of other table

alter table user_consent drop index IX_UserConsent_UserIdentifier;

alter table user_consent drop index UK_UserConsent_DocumentVersion_UserIdentifier;

alter table user_consent drop column id;

alter table user_consent drop column user_identifier;

alter table user_consent
    add column consents_user_id bigint not null;

alter table user_consent
    add constraint FK_UserConsent_ConsentsUser
        foreign key (consents_user_id)
            references consents_user (id);

create index IX_UserCons_ConsUser on user_consent (consents_user_id);

create table user_consent_delay
(
    id              bigint       not null auto_increment,
    delayed_at      datetime(6) not null,
    user_identifier varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table user_consent_delay
    add constraint IX_UserConsentDelay_UserIdentifier unique (user_identifier);
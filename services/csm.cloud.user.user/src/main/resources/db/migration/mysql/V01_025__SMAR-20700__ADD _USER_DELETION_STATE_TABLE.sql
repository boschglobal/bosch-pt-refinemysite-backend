create table user_deletion_state
(
    id                   bigint not null auto_increment,
    deleted_to_date_time datetime(6),
    primary key (id)
) engine=InnoDB;
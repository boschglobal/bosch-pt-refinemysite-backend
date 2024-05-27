alter table day_card
    modify column reason tinyint,
    modify column status tinyint;
alter table rfv_customization
    modify column rfv_key tinyint not null;
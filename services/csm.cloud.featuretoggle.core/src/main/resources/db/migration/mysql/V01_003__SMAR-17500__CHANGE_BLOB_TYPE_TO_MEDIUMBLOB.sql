ALTER TABLE featuretoggle_kafka_event
    MODIFY COLUMN event mediumblob;
ALTER TABLE featuretoggle_kafka_event
    MODIFY COLUMN event_key mediumblob not null;

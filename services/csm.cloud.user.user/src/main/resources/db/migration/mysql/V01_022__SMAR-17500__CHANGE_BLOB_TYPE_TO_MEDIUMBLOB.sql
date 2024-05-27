ALTER TABLE consents_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE consents_kafka_event MODIFY COLUMN event_key mediumblob not null;

ALTER TABLE craft_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE craft_kafka_event MODIFY COLUMN event_key mediumblob not null;

ALTER TABLE user_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE user_kafka_event MODIFY COLUMN event_key mediumblob not null;

ALTER TABLE company_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE company_kafka_event MODIFY COLUMN event_key mediumblob not null;

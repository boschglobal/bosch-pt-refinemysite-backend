alter table project_kafka_event modify event_key longblob not null;
alter table project_kafka_event modify partition_number integer not null;
alter table project_kafka_event modify trace_header_key varchar (255) not null;
alter table project_kafka_event modify trace_header_value varchar (255) not null;

alter table invitation_kafka_event modify event_key longblob not null;
alter table invitation_kafka_event modify partition_number integer not null;
alter table invitation_kafka_event modify trace_header_key varchar (255) not null;
alter table invitation_kafka_event modify trace_header_value varchar (255) not null;
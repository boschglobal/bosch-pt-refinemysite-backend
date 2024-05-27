alter table company_kafka_event_store
  add column trace_header_key varchar(255),
  add column trace_header_value varchar(255);

alter table craft_kafka_event_store
  add column trace_header_key varchar(255),
  add column trace_header_value varchar(255);

alter table project_kafka_event_store
  add column trace_header_key varchar(255),
  add column trace_header_value varchar(255);

alter table user_kafka_event_store
  add column trace_header_key varchar(255),
  add column trace_header_value varchar(255);
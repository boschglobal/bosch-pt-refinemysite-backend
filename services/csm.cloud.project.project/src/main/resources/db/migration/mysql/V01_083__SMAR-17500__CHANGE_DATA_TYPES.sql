ALTER TABLE invitation_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE invitation_kafka_event MODIFY COLUMN event_key mediumblob not null;

ALTER TABLE project_kafka_event MODIFY COLUMN event mediumblob;
ALTER TABLE project_kafka_event MODIFY COLUMN event_key mediumblob not null;

ALTER TABLE day_card MODIFY COLUMN manpower decimal (38,2) not null;
ALTER TABLE external_id MODIFY COLUMN id_type tinyint not null;
ALTER TABLE milestone MODIFY COLUMN type tinyint not null;
ALTER TABLE rfv_customization MODIFY COLUMN rfv_key tinyint not null;
ALTER TABLE task_constraint_customization MODIFY COLUMN tsk_con_key tinyint not null;

ALTER TABLE taskschedule_taskscheduleslot DROP FOREIGN KEY FK_TaskScheduleSlot_DayCard_DayCardId;
DROP INDEX UK_TaskScheduleSlot_DayCard ON taskschedule_taskscheduleslot;
ALTER TABLE taskschedule_taskscheduleslot
    ADD CONSTRAINT FK_TaskScheduleSlot_DayCard_DayCardId FOREIGN KEY (day_card_id) REFERENCES day_card (id);
CREATE INDEX IX_TaskTask_DayCard ON taskschedule_taskscheduleslot (day_card_id);
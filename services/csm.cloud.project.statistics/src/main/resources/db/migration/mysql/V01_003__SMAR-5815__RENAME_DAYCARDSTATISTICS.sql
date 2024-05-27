alter table day_card_statistics
  rename to day_card;

drop index IX_DayCardStatistics_ContIdentContTyp on day_card;
create index IX_DayCard_ContIdentContTyp on day_card (context_identifier, context_type);

create index IX_DayCard_TaskId on day_card (task_identifier);

drop index IX_DayCardStatistics_PrjIdDateStatus on day_card;

drop index UK_DayCardStatistics_PrjIdConIdentContTyp on day_card;
alter table day_card
  add constraint UK_DayCard_PrjIdConIdentContTyp unique (project_identifier, context_identifier, context_type);
alter table project_participant
   add constraint UK_ProjPart_Identifier unique (identifier);
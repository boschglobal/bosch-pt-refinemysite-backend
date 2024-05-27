alter table user_entity add column admin bit;

update user_entity set admin = 0;

alter table user_entity modify admin bit not null;
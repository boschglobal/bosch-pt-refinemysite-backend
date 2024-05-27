alter table company
  add column post_box_address_post_box varchar(255),
  add column post_box_address_area varchar(255),
  add column post_box_address_city varchar(255),
  add column post_box_address_country varchar(255),
  add column post_box_address_zip_code varchar(255),
  add column street_address_house_number varchar(255),
  add column street_address_street varchar(255),
  add column street_address_area varchar(255),
  add column street_address_city varchar(255),
  add column street_address_country varchar(255),
  add column street_address_zip_code varchar(255);

update company, address_postbox
  set company.post_box_address_post_box = address_postbox.post_box,
      company.post_box_address_zip_code = address_postbox.zip_code,
      company.post_box_address_city = address_postbox.city,
      company.post_box_address_area = address_postbox.area,
      company.post_box_address_country = address_postbox.country
  where company.post_box_address_id = address_postbox.id;

update company, address_street
  set company.street_address_house_number = address_street.house_number,
      company.street_address_street = address_street.street,
      company.street_address_zip_code = address_street.zip_code,
      company.street_address_city = address_street.city,
      company.street_address_area = address_street.area,
      company.street_address_country = address_street.country
  where company.street_address_id = address_street.id;

alter table company drop foreign key  FK_PostBoxAddress;
alter table company drop foreign key  FK_StreetAddress;

alter table company drop index IX_Compny_PostBoxAddr;
alter table company drop index IX_Compny_StreAddr;

alter table company drop column post_box_address_id;
alter table company drop column street_address_id;

drop table address_postbox;
drop table address_street;
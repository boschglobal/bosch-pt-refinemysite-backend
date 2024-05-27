alter table user_phonenumber
  add primary key(user_id, call_number, country_code, phone_number_type);